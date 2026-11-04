using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Windows.Media;

namespace WinOptimizerPro.Services;

public sealed class AppSettingsData
{
    public string ThemeId { get; set; } = "EmeraldCyber";
    public bool AutoOptimizeEnabled { get; set; } = false;
    public int AutoOptimizeThresholdPercent { get; set; } = 85;
    public int TelemetryIntervalMs { get; set; } = 1000;
    public bool FlushDnsOnClean { get; set; } = true;
    public bool PurgeStandbyOnOneClick { get; set; } = true;
}

public sealed record ColorThemeDefinition(
    string Id,
    string Name,
    string Subtitle,
    string BadgeText,
    Color WindowBg,
    Color CardBg,
    Color CardBorder,
    Color SubCardBg,
    Color SubCardBorder,
    Color LogBg,
    Color TextPrimary,
    Color TextSecondary,
    Color TextMuted,
    Color AccentPrimary,
    Color AccentPrimaryButtonBg,
    Color AccentPrimaryButtonFg,
    Color AccentSecondary,
    Color AccentSecondaryBg,
    Color AccentSecondaryBorder,
    Color AccentSecondaryFg,
    Color AccentTertiary,
    Color SecondaryButtonBg,
    Color SecondaryButtonHoverBg,
    Color SecondaryButtonBorder,
    Color BannerGradStart,
    Color BannerGradMid,
    Color BannerGradEnd,
    Color BannerBorder)
{
    public SolidColorBrush PreviewWindowBrush => new(WindowBg);
    public SolidColorBrush PreviewCardBrush => new(CardBg);
    public SolidColorBrush PreviewBorderBrush => new(CardBorder);
    public SolidColorBrush PreviewTextBrush => new(TextPrimary);
    public SolidColorBrush PreviewSubTextBrush => new(TextSecondary);
    public SolidColorBrush PreviewPrimaryBrush => new(AccentPrimary);
    public SolidColorBrush PreviewSecondaryBrush => new(AccentSecondary);
    public SolidColorBrush PreviewTertiaryBrush => new(AccentTertiary);
}

public sealed class ThemeCardItem
{
    public ColorThemeDefinition Theme { get; }
    public bool IsSelected { get; }

    public string Id => Theme.Id;
    public string Name => Theme.Name;
    public string Subtitle => Theme.Subtitle;
    public string BadgeText => Theme.BadgeText;

    public SolidColorBrush PreviewWindowBrush => Theme.PreviewWindowBrush;
    public SolidColorBrush PreviewCardBrush => Theme.PreviewCardBrush;
    public SolidColorBrush PreviewBorderBrush => Theme.PreviewBorderBrush;
    public SolidColorBrush PreviewTextBrush => Theme.PreviewTextBrush;
    public SolidColorBrush PreviewSubTextBrush => Theme.PreviewSubTextBrush;
    public SolidColorBrush PreviewPrimaryBrush => Theme.PreviewPrimaryBrush;
    public SolidColorBrush PreviewSecondaryBrush => Theme.PreviewSecondaryBrush;
    public SolidColorBrush PreviewTertiaryBrush => Theme.PreviewTertiaryBrush;

    public string StatusText => IsSelected ? "✓ TEMA ACTIVO" : "Aplicar este tema";
    public SolidColorBrush StatusBrush => IsSelected ? new SolidColorBrush(Theme.AccentPrimary) : new SolidColorBrush(Theme.TextSecondary);
    public SolidColorBrush SelectionBorderBrush => IsSelected ? new SolidColorBrush(Theme.AccentPrimary) : new SolidColorBrush(Theme.CardBorder);

    public ThemeCardItem(ColorThemeDefinition theme, bool isSelected)
    {
        Theme = theme;
        IsSelected = isSelected;
    }
}

public sealed class AppSettingsService
{
    private readonly string _settingsFilePath;
    private readonly List<ColorThemeDefinition> _themes;

    public AppSettingsData Current { get; private set; }

    public IReadOnlyList<ColorThemeDefinition> Themes => _themes;

    public AppSettingsService()
    {
        string appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        string folder = Path.Combine(appData, "WinOptimizerPro");
        _settingsFilePath = Path.Combine(folder, "settings.json");

        _themes = BuildThemes();
        Current = LoadSettings();
    }

    public ColorThemeDefinition GetThemeById(string? id)
    {
        if (!string.IsNullOrWhiteSpace(id))
        {
            foreach (var theme in _themes)
            {
                if (string.Equals(theme.Id, id, StringComparison.OrdinalIgnoreCase))
                {
                    return theme;
                }
            }
        }

        return _themes[0];
    }

    public void SaveSettings()
    {
        try
        {
            string? dir = Path.GetDirectoryName(_settingsFilePath);
            if (!string.IsNullOrWhiteSpace(dir) && !Directory.Exists(dir))
            {
                Directory.CreateDirectory(dir);
            }

            var json = JsonSerializer.Serialize(Current, new JsonSerializerOptions
            {
                WriteIndented = true
            });
            File.WriteAllText(_settingsFilePath, json);
        }
        catch
        {
            // Ignorar errores de escritura en disco restringido
        }
    }

    public void ResetToDefaults()
    {
        Current = new AppSettingsData();
        SaveSettings();
    }

    private AppSettingsData LoadSettings()
    {
        try
        {
            if (File.Exists(_settingsFilePath))
            {
                string json = File.ReadAllText(_settingsFilePath);
                var loaded = JsonSerializer.Deserialize<AppSettingsData>(json);
                if (loaded != null)
                {
                    loaded.AutoOptimizeThresholdPercent = Math.Clamp(loaded.AutoOptimizeThresholdPercent, 65, 95);
                    if (loaded.TelemetryIntervalMs is not (500 or 1000 or 2000))
                    {
                        loaded.TelemetryIntervalMs = 1000;
                    }

                    return loaded;
                }
            }
        }
        catch
        {
            // Fallback a configuración por defecto si el archivo está corrupto
        }

        return new AppSettingsData();
    }

    private static Color Hex(string hex) => (Color)ColorConverter.ConvertFromString(hex);

    private static List<ColorThemeDefinition> BuildThemes()
    {
        return new List<ColorThemeDefinition>
        {
            new(
                Id: "EmeraldCyber",
                Name: "Esmeralda Cyber",
                Subtitle: "Oscuro profundo con acentos verde neón y azul cian estilo AVG Cleaner.",
                BadgeText: "Predeterminado",
                WindowBg: Hex("#0D1117"),
                CardBg: Hex("#161B22"),
                CardBorder: Hex("#262C36"),
                SubCardBg: Hex("#0F141C"),
                SubCardBorder: Hex("#21262D"),
                LogBg: Hex("#0A0E14"),
                TextPrimary: Hex("#F0F6FC"),
                TextSecondary: Hex("#8B949E"),
                TextMuted: Hex("#6E7681"),
                AccentPrimary: Hex("#00E676"),
                AccentPrimaryButtonBg: Hex("#00C853"),
                AccentPrimaryButtonFg: Hex("#07120B"),
                AccentSecondary: Hex("#00B0FF"),
                AccentSecondaryBg: Hex("#112A3E"),
                AccentSecondaryBorder: Hex("#0091EA"),
                AccentSecondaryFg: Hex("#40C4FF"),
                AccentTertiary: Hex("#D2A8FF"),
                SecondaryButtonBg: Hex("#21262D"),
                SecondaryButtonHoverBg: Hex("#29323D"),
                SecondaryButtonBorder: Hex("#30363D"),
                BannerGradStart: Hex("#13222E"),
                BannerGradMid: Hex("#161B22"),
                BannerGradEnd: Hex("#122723"),
                BannerBorder: Hex("#263545")),

            new(
                Id: "ArcticOcean",
                Name: "Océano Ártico",
                Subtitle: "Azul marino profundo con acentos turquesa eléctrico y azul zafiro.",
                BadgeText: "Azul / Cian",
                WindowBg: Hex("#09111E"),
                CardBg: Hex("#111C2D"),
                CardBorder: Hex("#1F314D"),
                SubCardBg: Hex("#0B1524"),
                SubCardBorder: Hex("#192841"),
                LogBg: Hex("#070D17"),
                TextPrimary: Hex("#F0F7FF"),
                TextSecondary: Hex("#8CA3C0"),
                TextMuted: Hex("#627B9C"),
                AccentPrimary: Hex("#00E5FF"),
                AccentPrimaryButtonBg: Hex("#00B8D4"),
                AccentPrimaryButtonFg: Hex("#04151F"),
                AccentSecondary: Hex("#448AFF"),
                AccentSecondaryBg: Hex("#12264A"),
                AccentSecondaryBorder: Hex("#2979FF"),
                AccentSecondaryFg: Hex("#82B1FF"),
                AccentTertiary: Hex("#80D8FF"),
                SecondaryButtonBg: Hex("#17263D"),
                SecondaryButtonHoverBg: Hex("#213554"),
                SecondaryButtonBorder: Hex("#284066"),
                BannerGradStart: Hex("#0F2742"),
                BannerGradMid: Hex("#111C2D"),
                BannerGradEnd: Hex("#0E2B38"),
                BannerBorder: Hex("#23456B")),

            new(
                Id: "AmethystNeon",
                Name: "Amatista Neón",
                Subtitle: "Estética synthwave violeta oscuro con acentos púrpura neón y magenta.",
                BadgeText: "Púrpura / Synth",
                WindowBg: Hex("#0F0B18"),
                CardBg: Hex("#181226"),
                CardBorder: Hex("#2D2246"),
                SubCardBg: Hex("#120D1D"),
                SubCardBorder: Hex("#241B38"),
                LogBg: Hex("#0A0711"),
                TextPrimary: Hex("#F6F0FF"),
                TextSecondary: Hex("#9E90B8"),
                TextMuted: Hex("#73648F"),
                AccentPrimary: Hex("#E040FB"),
                AccentPrimaryButtonBg: Hex("#AA00FF"),
                AccentPrimaryButtonFg: Hex("#FFFFFF"),
                AccentSecondary: Hex("#00E5FF"),
                AccentSecondaryBg: Hex("#24143D"),
                AccentSecondaryBorder: Hex("#AB47BC"),
                AccentSecondaryFg: Hex("#EA80FC"),
                AccentTertiary: Hex("#FF80AB"),
                SecondaryButtonBg: Hex("#231A36"),
                SecondaryButtonHoverBg: Hex("#30234A"),
                SecondaryButtonBorder: Hex("#3B2C59"),
                BannerGradStart: Hex("#2A133D"),
                BannerGradMid: Hex("#181226"),
                BannerGradEnd: Hex("#191838"),
                BannerBorder: Hex("#432866")),

            new(
                Id: "CrimsonGaming",
                Name: "Carmesí Gaming",
                Subtitle: "Alto contraste en negro obsidiana con acentos rojo rubí y naranja.",
                BadgeText: "Rojo / Gaming",
                WindowBg: Hex("#110B0D"),
                CardBg: Hex("#1C1215"),
                CardBorder: Hex("#362026"),
                SubCardBg: Hex("#150D10"),
                SubCardBorder: Hex("#2B191E"),
                LogBg: Hex("#0C0709"),
                TextPrimary: Hex("#FFF0F3"),
                TextSecondary: Hex("#B08D95"),
                TextMuted: Hex("#82626A"),
                AccentPrimary: Hex("#FF1744"),
                AccentPrimaryButtonBg: Hex("#D50000"),
                AccentPrimaryButtonFg: Hex("#FFFFFF"),
                AccentSecondary: Hex("#FF9100"),
                AccentSecondaryBg: Hex("#33141B"),
                AccentSecondaryBorder: Hex("#FF1744"),
                AccentSecondaryFg: Hex("#FF8A80"),
                AccentTertiary: Hex("#FFD180"),
                SecondaryButtonBg: Hex("#29181D"),
                SecondaryButtonHoverBg: Hex("#382128"),
                SecondaryButtonBorder: Hex("#452830"),
                BannerGradStart: Hex("#2E1218"),
                BannerGradMid: Hex("#1C1215"),
                BannerGradEnd: Hex("#2B1912"),
                BannerBorder: Hex("#4D222B")),

            new(
                Id: "SolarAmber",
                Name: "Solar Dorado",
                Subtitle: "Carbón titanio con elegantes acentos en oro cálido y ámbar brillante.",
                BadgeText: "Ámbar / Oro",
                WindowBg: Hex("#12110E"),
                CardBg: Hex("#1C1915"),
                CardBorder: Hex("#332D24"),
                SubCardBg: Hex("#15130F"),
                SubCardBorder: Hex("#29241D"),
                LogBg: Hex("#0D0B09"),
                TextPrimary: Hex("#FDF8F0"),
                TextSecondary: Hex("#A89E8E"),
                TextMuted: Hex("#7A7163"),
                AccentPrimary: Hex("#FFAB00"),
                AccentPrimaryButtonBg: Hex("#FF8F00"),
                AccentPrimaryButtonFg: Hex("#140D02"),
                AccentSecondary: Hex("#00E5FF"),
                AccentSecondaryBg: Hex("#2E2412"),
                AccentSecondaryBorder: Hex("#FFAB00"),
                AccentSecondaryFg: Hex("#FFD740"),
                AccentTertiary: Hex("#FFE57F"),
                SecondaryButtonBg: Hex("#26221B"),
                SecondaryButtonHoverBg: Hex("#332E24"),
                SecondaryButtonBorder: Hex("#40392D"),
                BannerGradStart: Hex("#2B2111"),
                BannerGradMid: Hex("#1C1915"),
                BannerGradEnd: Hex("#1F2416"),
                BannerBorder: Hex("#473920")),

            new(
                Id: "NordicLight",
                Name: "Modo Claro Cristal",
                Subtitle: "Interfaz clara y luminosa estilo Windows 11 Light con verde esmeralda y azul.",
                BadgeText: "Modo Claro",
                WindowBg: Hex("#F2F5F9"),
                CardBg: Hex("#FFFFFF"),
                CardBorder: Hex("#D5DFEC"),
                SubCardBg: Hex("#F5F8FC"),
                SubCardBorder: Hex("#E1E8F2"),
                LogBg: Hex("#EDF2F8"),
                TextPrimary: Hex("#16202E"),
                TextSecondary: Hex("#526277"),
                TextMuted: Hex("#748499"),
                AccentPrimary: Hex("#00A854"),
                AccentPrimaryButtonBg: Hex("#009E4F"),
                AccentPrimaryButtonFg: Hex("#FFFFFF"),
                AccentSecondary: Hex("#0078D4"),
                AccentSecondaryBg: Hex("#E6F2FF"),
                AccentSecondaryBorder: Hex("#0078D4"),
                AccentSecondaryFg: Hex("#005A9E"),
                AccentTertiary: Hex("#7B2CBF"),
                SecondaryButtonBg: Hex("#EBF0F7"),
                SecondaryButtonHoverBg: Hex("#DCE5F2"),
                SecondaryButtonBorder: Hex("#C5D2E3"),
                BannerGradStart: Hex("#E3F5EC"),
                BannerGradMid: Hex("#FFFFFF"),
                BannerGradEnd: Hex("#E6F2FF"),
                BannerBorder: Hex("#B8D4E8"))
        };
    }
}
