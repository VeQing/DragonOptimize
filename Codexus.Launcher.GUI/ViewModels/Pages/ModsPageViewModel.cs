using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Codexus.Launcher.GUI.Services;

namespace Codexus.Launcher.GUI.ViewModels.Pages;

public partial class ModsPageViewModel : ViewModelBase
{
    private readonly ModService _service;
    public ObservableCollection<ModEntry> Mods => _service.Mods;

    [ObservableProperty] private ModEntry? _selected;

    public ModsPageViewModel(ModService service)
    {
        _service = service;
    }

    [RelayCommand]
    public void Refresh()
    {
        _service.RefreshList();
        if (Selected is null && Mods.Count > 0) Selected = Mods[0];
    }

    [RelayCommand]
    private void Toggle()
    {
        if (Selected is null) return;
        _service.Toggle(Selected);
    }

    [RelayCommand]
    private void Remove()
    {
        if (Selected is null) return;
        if (_service.Remove(Selected))
        {
            Mods.Remove(Selected);
            Selected = null;
        }
    }

    [RelayCommand]
    private async Task AddAsync()
    {
        // 实际的文件对话框由 View 层打开（参见 ModsPageView.axaml.cs）
        // View 拿到文件路径后调用 AddFromFile
        await Task.CompletedTask;
    }

    public void AddFromFile(string path)
    {
        if (_service.Add(path)) { /* 添加成功，调用方负责 Refresh */ }
    }
}
