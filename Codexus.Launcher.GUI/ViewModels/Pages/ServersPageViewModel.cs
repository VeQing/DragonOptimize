using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Codexus.Launcher.GUI.Services;

namespace Codexus.Launcher.GUI.ViewModels.Pages;

public partial class ServersPageViewModel : ViewModelBase
{
    private readonly LauncherStateService _state;

    public ObservableCollection<ServerEntry> Servers => _state.Servers;

    [ObservableProperty] private ServerEntry? _selected;

    public ServersPageViewModel(LauncherStateService state)
    {
        _state = state;
        if (Servers.Count > 0) Selected = Servers[0];
    }

    [RelayCommand]
    private void Add()
    {
        var entry = new ServerEntry { Name = "新服务器" };
        Servers.Add(entry);
        Selected = entry;
        _state.Save();
    }

    [RelayCommand]
    private void Remove()
    {
        if (Selected is null) return;
        Servers.Remove(Selected);
        Selected = Servers.Count > 0 ? Servers[0] : null;
        _state.Save();
    }

    [RelayCommand]
    private void Save()
    {
        _state.Save();
    }
}
