using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
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
    private readonly AppSettingsService _settingsService;
    private readonly DispatcherTimer _telemetryTimer;

    private readonly List<double> _ramHistory = new(HistoryMaxPoints);
    private readonly List<double> _cpuHistory = new(HistoryMaxPoints);
    private readonly ObservableCollection<string> _activityLog = new();

    private double _sessionFreedRamMb;
    private double _sessionCleanedCacheMb;
    private int _tickCount;
    private bool _isBusyOptimizing;
    private bool _isSyncingSettingsUi;
    private DateTime _lastAutoOptimizeUtc = DateTime.MinValue;

    private SolidColorBrush _currentAccentPrimaryBrush = new(Color.FromRgb(0x00, 0xE6, 0x76));
    private SolidColorBrush _currentAccentSecondaryBrush = new(Color.FromRgb(0x00, 0xB0, 0xFF));
    private static readonly SolidColorBrush BrushWarning = new(Color.FromRgb(0xFF, 0xB3, 0x00));
    private static readonly SolidColorBrush BrushCritical = new(Color.FromRgb(0xFF, 0x52, 0x52));

    public MainWindow()
    {
        InitializeComponent();

        _monitorService = new SystemMonitorService();
        _optimizerService = new OptimizerService();
        _settingsService = new AppSettingsService();

        ActivityLogListBox.ItemsSource = _activityLog;

        // Inicializar historiales en 0 para los gráficos de 60s
        for (int i = 0; i < HistoryMaxPoints; i++)
        {
            _ramHistory.Add(0);
            _cpuHistory.Add(0);
        }

        _telemetryTimer = new DispatcherTimer
        {
            Interval = TimeSpan.FromMilliseconds(_settingsService.Current.TelemetryIntervalMs)
        };
        _telemetryTimer.Tick += TelemetryTimer_Tick;

        LoadAndApplySettingsToUi(logTheme: false);
        ConfigureHeaderAndPrivilegeState();

        Loaded += MainWindow_Loaded;
    }

    private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        var activeTheme = _settingsService.GetThemeById(_settingsService.Current.ThemeId);
        AddLog($"Aplicación iniciada con el tema '{activeTheme.Name}'. Monitoreando RAM y CPU mediante Win32 API.");

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
            AdminElevateButton.Foreground = _currentAccentPrimaryBrush;
        }
        else
        {
            AdminElevateButton.Content = "🛡️ Elevar a Administrador (Standby List)";
            AdminElevateButton.ToolTip = "Reinicia la aplicación con permisos de Administrador para desbloquear el vaciado de la Standby List del kernel y Prefetch.";
        }
    }

    // =========================================================================
    // NAVEGACIÓN Y SISTEMA DE TEMAS / CONFIGURACIÓN
    // =========================================================================

    private void NavDashboardButton_Click(object sender, RoutedEventArgs e)
    {
        SwitchMainView(showSettings: false);
    }

    private void NavSettingsButton_Click(object sender, RoutedEventArgs e)
    {
        SwitchMainView(showSettings: true);
    }

    private void SwitchMainView(bool showSettings)
    {
        DashboardViewGrid.Visibility = showSettings ? Visibility.Collapsed : Visibility.Visible;
        SettingsViewScrollViewer.Visibility = showSettings ? Visibility.Visible : Visibility.Collapsed;

        var activeStyle = (Style)FindResource("CyanActionButton");
        var inactiveStyle = (Style)FindResource("SecondaryActionButton");

        NavDashboardButton.Style = showSettings ? inactiveStyle : activeStyle;
        NavSettingsButton.Style = showSettings ? activeStyle : inactiveStyle;
    }

    private void LoadAndApplySettingsToUi(bool logTheme)
    {
        _isSyncingSettingsUi = true;
        try
        {
            var cfg = _settingsService.Current;

            ApplyColorTheme(cfg.ThemeId, logChange: logTheme);

            AutoOptimizeCheckBox.IsChecked = cfg.AutoOptimizeEnabled;
            SettingsAutoOptimizeCheckBox.IsChecked = cfg.AutoOptimizeEnabled;

            AutoThresholdSlider.Value = cfg.AutoOptimizeThresholdPercent;
            UpdateAutoThresholdLabels(cfg.AutoOptimizeThresholdPercent);

            FlushDnsOnCleanCheckBox.IsChecked = cfg.FlushDnsOnClean;
            PurgeStandbyOnOneClickCheckBox.IsChecked = cfg.PurgeStandbyOnOneClick;

            UpdateCleanDiskButtonLabel(cfg.FlushDnsOnClean);
            UpdateTelemetryIntervalButtons(cfg.TelemetryIntervalMs);

            if (_telemetryTimer != null)
            {
                _telemetryTimer.Interval = TimeSpan.FromMilliseconds(cfg.TelemetryIntervalMs);
            }
        }
        finally
        {
            _isSyncingSettingsUi = false;
        }
    }

    private void ApplyColorTheme(string themeId, bool logChange)
    {
        var theme = _settingsService.GetThemeById(themeId);
        _settingsService.Current.ThemeId = theme.Id;

        _currentAccentPrimaryBrush = new SolidColorBrush(theme.AccentPrimary);
        _currentAccentSecondaryBrush = new SolidColorBrush(theme.AccentSecondary);

        Resources["WindowBgBrush"] = new SolidColorBrush(theme.WindowBg);
        Resources["CardBgBrush"] = new SolidColorBrush(theme.CardBg);
        Resources["CardBorderBrush"] = new SolidColorBrush(theme.CardBorder);
        Resources["SubCardBgBrush"] = new SolidColorBrush(theme.SubCardBg);
        Resources["SubCardBorderBrush"] = new SolidColorBrush(theme.SubCardBorder);
        Resources["LogBgBrush"] = new SolidColorBrush(theme.LogBg);
        Resources["TextPrimaryBrush"] = new SolidColorBrush(theme.TextPrimary);
        Resources["TextSecondaryBrush"] = new SolidColorBrush(theme.TextSecondary);
        Resources["TextMutedBrush"] = new SolidColorBrush(theme.TextMuted);

        Resources["AccentGreenBrush"] = _currentAccentPrimaryBrush;
        Resources["AccentPrimaryButtonBgBrush"] = new SolidColorBrush(theme.AccentPrimaryButtonBg);
        Resources["AccentPrimaryButtonFgBrush"] = new SolidColorBrush(theme.AccentPrimaryButtonFg);

        Resources["AccentCyanBrush"] = _currentAccentSecondaryBrush;
        Resources["AccentSecondaryBgBrush"] = new SolidColorBrush(theme.AccentSecondaryBg);
        Resources["AccentSecondaryBorderBrush"] = new SolidColorBrush(theme.AccentSecondaryBorder);
        Resources["AccentSecondaryFgBrush"] = new SolidColorBrush(theme.AccentSecondaryFg);

        Resources["AccentPurpleBrush"] = new SolidColorBrush(theme.AccentTertiary);

        Resources["SecondaryButtonBgBrush"] = new SolidColorBrush(theme.SecondaryButtonBg);
        Resources["SecondaryButtonHoverBgBrush"] = new SolidColorBrush(theme.SecondaryButtonHoverBg);
        Resources["SecondaryButtonBorderBrush"] = new SolidColorBrush(theme.SecondaryButtonBorder);

        Resources["BannerBorderBrush"] = new SolidColorBrush(theme.BannerBorder);
        Resources["RamChartAreaBrush"] = new SolidColorBrush(Color.FromArgb(0x2E, theme.AccentPrimary.R, theme.AccentPrimary.G, theme.AccentPrimary.B));
        Resources["CpuChartAreaBrush"] = new SolidColorBrush(Color.FromArgb(0x2E, theme.AccentSecondary.R, theme.AccentSecondary.G, theme.AccentSecondary.B));

        var logoGrad = new LinearGradientBrush
        {
            StartPoint = new Point(0, 0),
            EndPoint = new Point(1, 1)
        };
        logoGrad.GradientStops.Add(new GradientStop(theme.AccentPrimary, 0.0));
        logoGrad.GradientStops.Add(new GradientStop(theme.AccentSecondary, 1.0));
        Resources["LogoGradientBrush"] = logoGrad;

        var bannerGrad = new LinearGradientBrush
        {
            StartPoint = new Point(0, 0),
            EndPoint = new Point(1, 0)
        };
        bannerGrad.GradientStops.Add(new GradientStop(theme.BannerGradStart, 0.0));
        bannerGrad.GradientStops.Add(new GradientStop(theme.BannerGradMid, 0.55));
        bannerGrad.GradientStops.Add(new GradientStop(theme.BannerGradEnd, 1.0));
        Resources["BannerGradientBrush"] = bannerGrad;

        ActiveThemeHeaderBadge.Text = $"🎨 {theme.Name}";
        SelectedThemeSummaryText.Text = $"Tema activo: {theme.Name}";

        if (_optimizerService.IsRunningAsAdmin)
        {
            AdminElevateButton.Foreground = _currentAccentPrimaryBrush;
        }

        RefreshThemeCards(theme.Id);

        if (IsLoaded)
        {
            UpdateTelemetryUi();
        }

        if (logChange)
        {
            _settingsService.SaveSettings();
            AddLog($"[Configuración] Tema de color cambiado a '{theme.Name}' ({theme.BadgeText}).");
            LastActionBannerText.Text = $"🎨 Tema visual actualizado a '{theme.Name}'.";
        }
    }

    private void RefreshThemeCards(string selectedId)
    {
        var cards = new List<ThemeCardItem>(_settingsService.Themes.Count);
        foreach (var theme in _settingsService.Themes)
        {
            bool isSelected = string.Equals(theme.Id, selectedId, StringComparison.OrdinalIgnoreCase);
            cards.Add(new ThemeCardItem(theme, isSelected));
        }

        ThemesItemsControl.ItemsSource = cards;
    }

    private void ThemeCard_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
    {
        if (sender is FrameworkElement { Tag: string themeId })
        {
            ApplyColorTheme(themeId, logChange: true);
        }
    }

    private void SelectThemeButton_Click(object sender, RoutedEventArgs e)
    {
        if (sender is FrameworkElement { Tag: string themeId })
        {
            ApplyColorTheme(themeId, logChange: true);
            e.Handled = true;
        }
    }

    private void AutoOptimizeCheckBox_Changed(object sender, RoutedEventArgs e)
    {
        if (_isSyncingSettingsUi)
        {
            return;
        }

        _isSyncingSettingsUi = true;
        try
        {
            bool enabled = AutoOptimizeCheckBox.IsChecked == true;
            SettingsAutoOptimizeCheckBox.IsChecked = enabled;
            _settingsService.Current.AutoOptimizeEnabled = enabled;
            _settingsService.SaveSettings();

            AddLog(enabled
                ? $"[Configuración] Auto-optimización de RAM activada (umbral >{_settingsService.Current.AutoOptimizeThresholdPercent}%)."
                : "[Configuración] Auto-optimización de RAM desactivada.");
        }
        finally
        {
            _isSyncingSettingsUi = false;
        }
    }

    private void SettingsAutoOptimizeCheckBox_Changed(object sender, RoutedEventArgs e)
    {
        if (_isSyncingSettingsUi)
        {
            return;
        }

        _isSyncingSettingsUi = true;
        try
        {
            bool enabled = SettingsAutoOptimizeCheckBox.IsChecked == true;
            AutoOptimizeCheckBox.IsChecked = enabled;
            _settingsService.Current.AutoOptimizeEnabled = enabled;
            _settingsService.SaveSettings();

            AddLog(enabled
                ? $"[Configuración] Auto-optimización de RAM activada (umbral >{_settingsService.Current.AutoOptimizeThresholdPercent}%)."
                : "[Configuración] Auto-optimización de RAM desactivada.");
        }
        finally
        {
            _isSyncingSettingsUi = false;
        }
    }

    private void AutoThresholdSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (!IsLoaded && _isSyncingSettingsUi)
        {
            return;
        }

        int threshold = (int)Math.Round(e.NewValue);
        UpdateAutoThresholdLabels(threshold);

        if (!_isSyncingSettingsUi && _settingsService != null)
        {
            _settingsService.Current.AutoOptimizeThresholdPercent = threshold;
            _settingsService.SaveSettings();
        }
    }

    private void UpdateAutoThresholdLabels(int threshold)
    {
        if (AutoThresholdValueText != null)
        {
            AutoThresholdValueText.Text = $"{threshold}%";
        }

        if (AutoOptimizeCheckBox != null)
        {
            AutoOptimizeCheckBox.Content = $"Auto-liberar RAM (>{threshold}%)";
        }
    }

    private void CleaningOptionsCheckBox_Changed(object sender, RoutedEventArgs e)
    {
        if (_isSyncingSettingsUi || _settingsService == null)
        {
            return;
        }

        bool flushDns = FlushDnsOnCleanCheckBox.IsChecked == true;
        bool purgeStandby = PurgeStandbyOnOneClickCheckBox.IsChecked == true;

        _settingsService.Current.FlushDnsOnClean = flushDns;
        _settingsService.Current.PurgeStandbyOnOneClick = purgeStandby;
        _settingsService.SaveSettings();

        UpdateCleanDiskButtonLabel(flushDns);
    }

    private void UpdateCleanDiskButtonLabel(bool flushDns)
    {
        if (CleanDiskCacheButton != null)
        {
            CleanDiskCacheButton.Content = flushDns
                ? "🗑️ Limpiar Caché Temporal + Prefetch + DNS"
                : "🗑️ Limpiar Caché Temporal + Prefetch";
        }
    }

    private void TelemetryIntervalButton_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button { Tag: string tagStr } && int.TryParse(tagStr, out int intervalMs))
        {
            _settingsService.Current.TelemetryIntervalMs = intervalMs;
            _settingsService.SaveSettings();

            _telemetryTimer.Interval = TimeSpan.FromMilliseconds(intervalMs);
            UpdateTelemetryIntervalButtons(intervalMs);

            AddLog($"[Configuración] Intervalo de actualización del monitor ajustado a {intervalMs / 1000.0:N1}s.");
        }
    }

    private void UpdateTelemetryIntervalButtons(int intervalMs)
    {
        var activeStyle = (Style)FindResource("CyanActionButton");
        var inactiveStyle = (Style)FindResource("SecondaryActionButton");

        IntervalFastButton.Style = intervalMs == 500 ? activeStyle : inactiveStyle;
        IntervalNormalButton.Style = intervalMs == 1000 ? activeStyle : inactiveStyle;
        IntervalEcoButton.Style = intervalMs == 2000 ? activeStyle : inactiveStyle;
    }

    private void ResetSettingsButton_Click(object sender, RoutedEventArgs e)
    {
        _settingsService.ResetToDefaults();
        LoadAndApplySettingsToUi(logTheme: true);
        AddLog("[Configuración] Se han restaurado todos los ajustes y el tema predeterminado (Esmeralda Cyber).");
    }

    // =========================================================================
    // TELEMETRÍA Y GRÁFICOS EN TIEMPO REAL
    // =========================================================================

    private async void TelemetryTimer_Tick(object? sender, EventArgs e)
    {
        _tickCount++;
        var snapshot = UpdateTelemetryUi();

        // Actualizar lista de procesos cada 4 ciclos
        if (_tickCount % 4 == 0 && !_isBusyOptimizing)
        {
            await RefreshTopProcessesAsync();
        }

        int threshold = _settingsService.Current.AutoOptimizeThresholdPercent;

        // Auto-optimización inteligente si supera el umbral configurado y tiene cooldown de 60s
        if (AutoOptimizeCheckBox.IsChecked == true &&
            !_isBusyOptimizing &&
            snapshot.RamUsagePercent >= threshold &&
            (DateTime.UtcNow - _lastAutoOptimizeUtc).TotalSeconds >= 60)
        {
            _lastAutoOptimizeUtc = DateTime.UtcNow;
            AddLog($"[Auto-Optimización] Uso de RAM alto ({snapshot.RamUsagePercent:N1}% >= {threshold}%). Liberando Working Sets inactivos...");
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

        // Actualizar colores y medidores circulares según el tema activo
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

    private SolidColorBrush GetLoadBrush(double percent, bool isCpu)
    {
        if (percent >= 85.0)
        {
            return BrushCritical;
        }

        if (percent >= 68.0)
        {
            return BrushWarning;
        }

        return isCpu ? _currentAccentSecondaryBrush : _currentAccentPrimaryBrush;
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
            HealthStatusDot.Fill = _currentAccentPrimaryBrush;
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
    // ACCIONES DE USUARIO (BOTONES DE OPTIMIZACIÓN)
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

            // 2. Purgar Standby List si es Administrador y está habilitado en Configuración
            double standbyMb = 0;
            if (_optimizerService.IsRunningAsAdmin && _settingsService.Current.PurgeStandbyOnOneClick)
            {
                var standbyResult = await _optimizerService.PurgeRamStandbyCacheAsync();
                standbyMb = standbyResult.CachePurgedMb;
                _sessionFreedRamMb += standbyResult.FreedRamMb;
                AddLog(standbyResult.SummaryMessage);
            }

            // 3. Limpiar Caché Temporal, Prefetch y (opcionalmente) DNS
            bool flushDns = _settingsService.Current.FlushDnsOnClean;
            var diskResult = await _optimizerService.CleanSystemCacheAsync(flushDns);
            _sessionCleanedCacheMb += diskResult.FreedDiskMb;
            AddLog(diskResult.SummaryMessage);

            UpdateSessionCountersUi();
            UpdateTelemetryUi();
            await RefreshTopProcessesAsync();

            var postScan = await _optimizerService.ScanSystemCacheAsync();
            DiskCacheSizeText.Text = $"{postScan.TotalSizeMb:N1} MB";

            double totalRamGain = ramResult.FreedRamMb + standbyMb;
            string dnsSuffix = diskResult.DnsFlushed ? " (+ DNS vaciada)" : "";
            LastActionBannerText.Text =
                $"✅ Optimización completada: {totalRamGain:N1} MB de RAM/Standby optimizados y {diskResult.FreedDiskMb:N1} MB de caché eliminados{dnsSuffix}.";

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
            bool flushDns = _settingsService.Current.FlushDnsOnClean;
            var clean = await _optimizerService.CleanSystemCacheAsync(flushDns);
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
