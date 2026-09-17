using System.Collections.ObjectModel;
using System.IO;
using Codexus.ModHost;
using Serilog;

namespace Codexus.Launcher.GUI.Services;

/// <summary>
/// 托管 ModManager 的扫描/启用/禁用/添加，给 UI 提供一个 ObservableCollection。
/// </summary>
public sealed class ModService
{
    private readonly ModManager _manager;
    private readonly ILogger _logger;

    public ObservableCollection<ModEntry> Mods { get; } = new();
    public string ModsDirectory { get; }

    public ModService(ILogger logger, string? modsDir = null)
    {
        _logger = logger;
        ModsDirectory = modsDir ?? Path.Combine(AppContext.BaseDirectory, "Mods");
        _manager = new ModManager(logger, ModsDirectory);
        if (!Directory.Exists(ModsDirectory))
        {
            try { Directory.CreateDirectory(ModsDirectory); }
            catch (Exception ex) { _logger.Warning(ex, "Failed to create Mods dir at {Dir}", ModsDirectory); }
        }
    }

    public void Initialize()
    {
        try
        {
            _manager.Initialize();
            RefreshList();
        }
        catch (Exception ex)
        {
            _logger.Warning(ex, "Mod manager initialize failed, continuing without mods.");
        }
    }

    public void RefreshList()
    {
        Mods.Clear();
        try
        {
            var files = Directory.EnumerateFiles(ModsDirectory, "*.dll", SearchOption.TopDirectoryOnly);
            foreach (var f in files)
            {
                Mods.Add(new ModEntry
                {
                    FilePath = f,
                    FileName = Path.GetFileName(f),
                    Enabled = !f.EndsWith(".disabled", StringComparison.OrdinalIgnoreCase),
                    SizeBytes = new FileInfo(f).Length
                });
            }
        }
        catch (Exception ex)
        {
            _logger.Warning(ex, "Refresh Mods list failed.");
        }
    }

    public bool Toggle(ModEntry entry)
    {
        try
        {
            var newPath = entry.Enabled
                ? entry.FilePath + ".disabled"
                : entry.FilePath.Replace(".disabled", "", StringComparison.OrdinalIgnoreCase);
            File.Move(entry.FilePath, newPath);
            entry.FilePath = newPath;
            entry.Enabled = !entry.Enabled;
            return true;
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Toggle mod {Name} failed", entry.FileName);
            return false;
        }
    }

    public bool Add(string sourcePath)
    {
        try
        {
            var dest = Path.Combine(ModsDirectory, Path.GetFileName(sourcePath));
            File.Copy(sourcePath, dest, overwrite: true);
            _logger.Information("Copied mod {Name}", Path.GetFileName(sourcePath));
            return true;
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Add mod failed: {Path}", sourcePath);
            return false;
        }
    }

    public bool Remove(ModEntry entry)
    {
        try
        {
            if (File.Exists(entry.FilePath))
                File.Delete(entry.FilePath);
            return true;
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Remove mod {Name} failed", entry.FileName);
            return false;
        }
    }
}

public sealed class ModEntry
{
    public string FilePath { get; set; } = "";
    public string FileName { get; set; } = "";
    public bool Enabled { get; set; }
    public long SizeBytes { get; set; }
}
