using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Shapes;
using System.Windows.Threading;
using WinOptimizerPro.Services;

namespace WinOptimizerPro;

public partial class MainWindow : Window
{
    private const int HistoryMaxPoints = 60;

    private readonly SystemMonitorService _monitorService;
    private readonly OptimizerService _optimizerService;
    private readonly DispatcherTimer _telemetryTimer;

    private readonly List<double> _ramHistory = new(HistoryMaxPoints);
    private readonly List<double> _cpuHistory = new(HistoryMaxPoints);
    private readonly ObservableCollection<string> _activityLog = new();

    private double _sessionFreedRamMb;
    private double _sessionCleanedCacheMb;
    private int _tickCount;
    private bool _isBusyOptimizing;
    private DateTime _lastAutoOptimizeUtc = DateTime.MinValue;

    private static readonly SolidColorBrush BrushOptimal = new(Color.FromRgb(0x00, 0xE6, 0x76));
    private static readonly SolidColorBrush BrushCyan = new(Color.FromRgb(0x00, 0xB0, 0xFF));
    private static readonly SolidColorBrush BrushWarning = new(Color.FromRgb(0xFF, 0xB3, 0x00));
    private static readonly SolidColorBrush BrushCritical = new(Color.FromRgb(0xFF, 0x52, 0x52));

    public MainWindow()
    {
        InitializeComponent();

        _monitorService = new SystemMonitorService();
        _optimizerService = new OptimizerService();

        ActivityLogListBox.ItemsSource = _activityLog;

        // Inicializar historiales en 0 para los gráficos de 60s
        for (int i = 0; i < HistoryMaxPoints; i++)
        {
            _ramHistory.Add(0);
            _cpuHistory.Add(0);
        }

        ConfigureHeaderAndPrivilegeState();

        _telemetryTimer = new DispatcherTimer
        {
            Interval = TimeSpan.FromSeconds(1)
        };
        _telemetryTimer.Tick += TelemetryTimer_Tick;

        Loaded += MainWindow_Loaded;
    }

    private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        AddLog("Aplicación iniciada. Monitoreando RAM y CPU mediante Win32 API (GlobalMemoryStatusEx / GetSystemTimes).");

        if (_optimizerService.IsRunningAsAdmin)
        {
            AddLog("Permisos de Administrador activos: Purga profunda de Standby List (NtSetSystemInformation) habilitada.");
        }
        else
        {
            AddLog("Modo Usuario estándar activo: Liberación de Working Sets habilitada. Puedes elevar a Administrador para purgar la Standby List del kernel.");
        }

        UpdateTelemetryUi();
        await RefreshTopProcessesAsync();
        _telemetryTimer.Start();

        // Escaneo inicial rápido de caché en segundo plano
        var scan = await _optimizerService.ScanSystemCacheAsync();
        DiskCacheSizeText.Text = $"{scan.TotalSizeMb:N1} MB";
    }

    private void ConfigureHeaderAndPrivilegeState()
    {
        CpuModelHeaderLabel.Text = _monitorService.CpuModelName;
        CpuCoresShortText.Text = $"{_monitorService.LogicalCores} Núcleos Lógicos";

        if (_optimizerService.IsRunningAsAdmin)
        {
            AdminElevateButton.Content = "⚡ Modo Administrador Activo";
            AdminElevateButton.IsEnabled = false;
            AdminElevateButton.Foreground = BrushOptimal;
        }
        else
        {
            AdminElevateButton.Content = "🛡️ Elevar a Administrador (Standby List)";
            AdminElevateButton.ToolTip = "Reinicia la aplicación con permisos de Administrador para desbloquear el vaciado de la Standby List del kernel y Prefetch.";
        }
    }

    private async void TelemetryTimer_Tick(object? sender, EventArgs e)
    {
        _tickCount++;
        var snapshot = UpdateTelemetryUi();

        // Actualizar lista de procesos cada 4 segundos
        if (_tickCount % 4 == 0 && !_isBusyOptimizing)
        {
            await RefreshTopProcessesAsync();
        }

        // Auto-optimización inteligente si supera el 85% y tiene cooldown de 60s
        if (AutoOptimizeCheckBox.IsChecked == true &&
            !_isBusyOptimizing &&
            snapshot.RamUsagePercent >= 85.0 &&
            (DateTime.UtcNow - _lastAutoOptimizeUtc).TotalSeconds >= 60)
        {
            _lastAutoOptimizeUtc = DateTime.UtcNow;
            AddLog($"[Auto-Optimización] Uso de RAM alto ({snapshot.RamUsagePercent:N1}%). Liberando Working Sets inactivos...");
            await ExecuteReleaseRamAsync();
        }
    }

    private SystemMetricsSnapshot UpdateTelemetryUi()
    {
        var snap = _monitorService.GetMetricsSnapshot();

        // Actualizar etiquetas de RAM
        RamTotalHeaderLabel.Text = $"{snap.TotalRamGb:N1} GB Totales";
        RamPercentText.Text = $"{snap.RamUsagePercent:N0}%";
        RamUsedShortText.Text = $"{snap.UsedRamGb:N1} / {snap.TotalRamGb:N1} GB";
        RamUsedDetailText.Text = $"{snap.UsedRamGb:N2} GB";
        RamAvailableDetailText.Text = $"{snap.AvailableRamGb:N2} GB";
        RamCacheDetailText.Text = $"{snap.SystemCacheGb:N2} GB";
        RamCommitDetailText.Text = $"{snap.CommitTotalGb:N1} / {snap.CommitLimitGb:N1} GB";

        // Actualizar etiquetas de CPU
        CpuPercentText.Text = $"{snap.CpuUsagePercent:N0}%";
        CpuProcessesText.Text = snap.ProcessCount.ToString("N0");
        CpuThreadsText.Text = snap.ThreadCount.ToString("N0");
        CpuHandlesText.Text = snap.HandleCount.ToString("N0");

        // Actualizar colores y medidores circulares
        var ramBrush = GetLoadBrush(snap.RamUsagePercent, isCpu: false);
        var cpuBrush = GetLoadBrush(snap.CpuUsagePercent, isCpu: true);

        RamGaugeArc.Stroke = ramBrush;
        CpuGaugeArc.Stroke = cpuBrush;

        UpdateCircularArc(RamGaugeArc, snap.RamUsagePercent, 126, 126, 58);
        UpdateCircularArc(CpuGaugeArc, snap.CpuUsagePercent, 126, 126, 58);

        // Actualizar estado global de salud
        UpdateSystemHealthIndicator(snap.RamUsagePercent, snap.CpuUsagePercent);

        // Actualizar historiales de 60 segundos
        PushHistory(_ramHistory, snap.RamUsagePercent);
        PushHistory(_cpuHistory, snap.CpuUsagePercent);

        RenderChart(RamChartCanvas, RamLinePolyline, RamAreaPolygon, _ramHistory);
        RenderChart(CpuChartCanvas, CpuLinePolyline, CpuAreaPolygon, _cpuHistory);

        return snap;
    }

    private static SolidColorBrush GetLoadBrush(double percent, bool isCpu)
    {
        if (percent >= 85.0)
        {
            return BrushCritical;
        }

        if (percent >= 68.0)
        {
            return BrushWarning;
        }

        return isCpu ? BrushCyan : BrushOptimal;
    }

    private void UpdateSystemHealthIndicator(double ramPercent, double cpuPercent)
    {
        if (ramPercent >= 85.0 || cpuPercent >= 90.0)
        {
            HealthStatusDot.Fill = BrushCritical;
            HealthStatusTitle.Text = $"Estado del Sistema: Carga Elevada (RAM {ramPercent:N0}% • CPU {cpuPercent:N0}%)";
        }
        else if (ramPercent >= 68.0 || cpuPercent >= 70.0)
        {
            HealthStatusDot.Fill = BrushWarning;
            HealthStatusTitle.Text = $"Estado del Sistema: Carga Moderada (RAM {ramPercent:N0}% • CPU {cpuPercent:N0}%)";
        }
        else
        {
            HealthStatusDot.Fill = BrushOptimal;
            HealthStatusTitle.Text = $"Estado del Sistema: Rendimiento Óptimo (RAM {ramPercent:N0}% • CPU {cpuPercent:N0}%)";
        }
    }

    private static void PushHistory(List<double> history, double value)
    {
        if (history.Count >= HistoryMaxPoints)
        {
            history.RemoveAt(0);
        }

        history.Add(Math.Clamp(value, 0.0, 100.0));
    }

    private static void UpdateCircularArc(Path pathElement, double percentage, double width, double height, double radius)
    {
        double clamped = Math.Clamp(percentage, 0.5, 99.9);
        double angle = (clamped / 100.0) * 360.0;

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        double startAngleRad = -Math.PI / 2.0;
        double endAngleRad = startAngleRad + (angle * Math.PI / 180.0);

        Point startPoint = new(
            centerX + radius * Math.Cos(startAngleRad),
            centerY + radius * Math.Sin(startAngleRad));

        Point endPoint = new(
            centerX + radius * Math.Cos(endAngleRad),
            centerY + radius * Math.Sin(endAngleRad));

        bool isLargeArc = angle > 180.0;

        var arcSegment = new ArcSegment(
            endPoint,
            new Size(radius, radius),
            0,
            isLargeArc,
            SweepDirection.Clockwise,
            true);

        var figure = new PathFigure(startPoint, new[] { arcSegment }, false);
        var geometry = new PathGeometry(new[] { figure });
        pathElement.Data = geometry;
    }

    private static void RenderChart(Canvas canvas, Polyline line, Polygon area, List<double> values)
    {
        double w = canvas.ActualWidth;
        double h = canvas.ActualHeight;
        if (w <= 10 || h <= 10 || values.Count < 2)
        {
            return;
        }

        var linePoints = new PointCollection(values.Count);
        var areaPoints = new PointCollection(values.Count + 2)
        {
            new Point(0, h)
        };

        double stepX = w / (HistoryMaxPoints - 1);

        for (int i = 0; i < values.Count; i++)
        {
            double x = i * stepX;
            double normalized = Math.Clamp(values[i] / 100.0, 0.0, 1.0);
            double y = h - (normalized * (h - 6)) - 3;

            var pt = new Point(x, y);
            linePoints.Add(pt);
            areaPoints.Add(pt);
        }

        areaPoints.Add(new Point(w, h));

        line.Points = linePoints;
        area.Points = areaPoints;
    }

    private void RamChartCanvas_SizeChanged(object sender, SizeChangedEventArgs e)
    {
        RenderChart(RamChartCanvas, RamLinePolyline, RamAreaPolygon, _ramHistory);
    }

    private void CpuChartCanvas_SizeChanged(object sender, SizeChangedEventArgs e)
    {
        RenderChart(CpuChartCanvas, CpuLinePolyline, CpuAreaPolygon, _cpuHistory);
    }

    private async Task RefreshTopProcessesAsync()
    {
        var topProcesses = await Task.Run(() => _monitorService.GetTopRamProcesses(12));
        TopProcessesListBox.ItemsSource = topProcesses;
    }

    private void SetButtonsEnabled(bool enabled)
    {
        _isBusyOptimizing = !enabled;
        OneClickOptimizeButton.IsEnabled = enabled;
        ReleaseRamButton.IsEnabled = enabled;
        PurgeStandbyButton.IsEnabled = enabled;
        ScanDiskCacheButton.IsEnabled = enabled;
        CleanDiskCacheButton.IsEnabled = enabled;
        RefreshProcessesButton.IsEnabled = enabled;
    }

    private void AddLog(string message)
    {
        string timestamped = $"[{DateTime.Now:HH:mm:ss}] {message}";
        _activityLog.Insert(0, timestamped);

        while (_activityLog.Count > 100)
        {
            _activityLog.RemoveAt(_activityLog.Count - 1);
        }
    }

    private void UpdateSessionCountersUi()
    {
        SessionRamFreedText.Text = _sessionFreedRamMb >= 1024
            ? $"{_sessionFreedRamMb / 1024.0:N2} GB"
            : $"{_sessionFreedRamMb:N1} MB";

        SessionCacheCleanedText.Text = _sessionCleanedCacheMb >= 1024
            ? $"{_sessionCleanedCacheMb / 1024.0:N2} GB"
            : $"{_sessionCleanedCacheMb:N1} MB";
    }

    // =========================================================================
    // ACCIONES DE USUARIO (BOTONES)
    // =========================================================================

    private async void OneClickOptimizeButton_Click(object sender, RoutedEventArgs e)
    {
        SetButtonsEnabled(false);
        string originalText = OneClickOptimizeButton.Content?.ToString() ?? "🚀 OPTIMIZAR TODO (1-CLIC)";
        OneClickOptimizeButton.Content = "⏳ OPTIMIZANDO SISTEMA...";

        try
        {
            AddLog("=== Iniciando Optimización Integral 1-Clic ===");

            // 1. Liberar Working Sets de RAM
            var ramResult = await _optimizerService.ReleaseRamWorkingSetsAsync();
            _sessionFreedRamMb += ramResult.FreedRamMb;
            AddLog(ramResult.SummaryMessage);

            // 2. Purgar Standby List si es Administrador
            double standbyMb = 0;
            if (_optimizerService.IsRunningAsAdmin)
            {
                var standbyResult = await _optimizerService.PurgeRamStandbyCacheAsync();
                standbyMb = standbyResult.CachePurgedMb;
                _sessionFreedRamMb += standbyResult.FreedRamMb;
                AddLog(standbyResult.SummaryMessage);
            }

            // 3. Limpiar Caché Temporal, Prefetch y DNS
            var diskResult = await _optimizerService.CleanSystemCacheAsync();
            _sessionCleanedCacheMb += diskResult.FreedDiskMb;
            AddLog(diskResult.SummaryMessage);

            UpdateSessionCountersUi();
            UpdateTelemetryUi();
            await RefreshTopProcessesAsync();

            var postScan = await _optimizerService.ScanSystemCacheAsync();
            DiskCacheSizeText.Text = $"{postScan.TotalSizeMb:N1} MB";

            double totalRamGain = ramResult.FreedRamMb + standbyMb;
            LastActionBannerText.Text =
                $"✅ Optimización completada: {totalRamGain:N1} MB de RAM/Standby optimizados y {diskResult.FreedDiskMb:N1} MB de caché eliminados (+ DNS vaciada).";

            AddLog("=== Optimización Integral 1-Clic finalizada con éxito ===");
        }
        finally
        {
            OneClickOptimizeButton.Content = originalText;
            SetButtonsEnabled(true);
        }
    }

    private async void ReleaseRamButton_Click(object sender, RoutedEventArgs e)
    {
        await ExecuteReleaseRamAsync();
    }

    private async Task ExecuteReleaseRamAsync()
    {
        SetButtonsEnabled(false);
        try
        {
            var result = await _optimizerService.ReleaseRamWorkingSetsAsync();
            _sessionFreedRamMb += result.FreedRamMb;
            UpdateSessionCountersUi();

            LastActionBannerText.Text = $"✅ {result.SummaryMessage}";
            AddLog(result.SummaryMessage);

            UpdateTelemetryUi();
            await RefreshTopProcessesAsync();
        }
        finally
        {
            SetButtonsEnabled(true);
        }
    }

    private async void PurgeStandbyButton_Click(object sender, RoutedEventArgs e)
    {
        SetButtonsEnabled(false);
        try
        {
            var result = await _optimizerService.PurgeRamStandbyCacheAsync();
            if (result.Success)
            {
                _sessionFreedRamMb += result.CachePurgedMb;
                UpdateSessionCountersUi();
                LastActionBannerText.Text = $"✅ {result.SummaryMessage}";
            }
            else
            {
                LastActionBannerText.Text = $"ℹ️ {result.SummaryMessage}";
            }

            AddLog(result.SummaryMessage);
            UpdateTelemetryUi();
        }
        finally
        {
            SetButtonsEnabled(true);
        }
    }

    private async void ScanDiskCacheButton_Click(object sender, RoutedEventArgs e)
    {
        SetButtonsEnabled(false);
        try
        {
            DiskCacheSizeText.Text = "Escaneando...";
            var scan = await _optimizerService.ScanSystemCacheAsync();
            DiskCacheSizeText.Text = $"{scan.TotalSizeMb:N1} MB";
            LastActionBannerText.Text = $"🔍 {scan.DetailsMessage}";
            AddLog(scan.DetailsMessage);
        }
        finally
        {
            SetButtonsEnabled(true);
        }
    }

    private async void CleanDiskCacheButton_Click(object sender, RoutedEventArgs e)
    {
        SetButtonsEnabled(false);
        try
        {
            var clean = await _optimizerService.CleanSystemCacheAsync();
            _sessionCleanedCacheMb += clean.FreedDiskMb;
            UpdateSessionCountersUi();

            LastActionBannerText.Text = $"✅ {clean.SummaryMessage}";
            AddLog(clean.SummaryMessage);

            var scanAfter = await _optimizerService.ScanSystemCacheAsync();
            DiskCacheSizeText.Text = $"{scanAfter.TotalSizeMb:N1} MB";
        }
        finally
        {
            SetButtonsEnabled(true);
        }
    }

    private async void TrimSingleProcessButton_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button { Tag: ProcessMemoryItem item })
        {
            var (success, freedMb, message) = _optimizerService.TrimSingleProcess(item.Pid, item.Name);
            if (success && freedMb > 0)
            {
                _sessionFreedRamMb += freedMb;
                UpdateSessionCountersUi();
            }

            LastActionBannerText.Text = success ? $"✅ {message}" : $"ℹ️ {message}";
            AddLog(message);

            UpdateTelemetryUi();
            await RefreshTopProcessesAsync();
        }
    }

    private async void RefreshProcessesButton_Click(object sender, RoutedEventArgs e)
    {
        await RefreshTopProcessesAsync();
        AddLog("Lista de procesos con mayor uso de RAM actualizada.");
    }

    private void ClearLogButton_Click(object sender, RoutedEventArgs e)
    {
        _activityLog.Clear();
        AddLog("Registro de eventos limpiado.");
    }

    private void AdminElevateButton_Click(object sender, RoutedEventArgs e)
    {
        if (_optimizerService.IsRunningAsAdmin)
        {
            return;
        }

        if (_optimizerService.TryRestartAsAdministrator())
        {
            Application.Current.Shutdown();
        }
        else
        {
            AddLog("Elevación a Administrador cancelada por el usuario.");
        }
    }
}