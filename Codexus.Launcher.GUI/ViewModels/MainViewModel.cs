using System.Collections.ObjectModel;
using Avalonia.Controls;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Codexus.Launcher.GUI.Services;
using Codexus.Launcher.GUI.ViewModels.Pages;
using Codexus.OpenSDK.Authentication;
using Serilog;

namespace Codexus.Launcher.GUI.ViewModels;

public partial class MainViewModel : ViewModelBase, IDisposable
{
    private readonly LauncherStateService _state;
    private readonly AuthManager _auth = new();
    private readonly TransportService _transport;
    private readonly ModService _mods;
    private readonly UiLogSink _log;

    public ObservableCollection<PageItem> Pages { get; } = new();

    /// <summary>当前显示的页面 ViewModel（ContentControl 绑这个）</summary>
    [ObservableProperty]
    private ViewModelBase? _currentView;

    [ObservableProperty]
    private PageItem? _currentPage;

    public AuthPageViewModel Auth { get; }
    public ServersPageViewModel Servers { get; }
    public ModsPageViewModel Mods { get; }
    public ConsolePageViewModel Console { get; }

    public MainViewModel()
    {
        _log = new UiLogSink();
        var logger = new LoggerConfiguration()
            .MinimumLevel.Debug()
            .WriteTo.Sink(_log)
            .CreateLogger();
        Log.Logger = logger;

        _state = new LauncherStateService();
        _transport = new TransportService(logger, _log);
        _mods = new ModService(logger);
        _mods.Initialize();

        Auth = new AuthPageViewModel(_auth, _state, _transport, logger);
        Servers = new ServersPageViewModel(_state);
        Mods = new ModsPageViewModel(_mods);
        Console = new ConsolePageViewModel(_transport, _log);

        Pages.Add(new PageItem("认证", Auth));
        Pages.Add(new PageItem("服务器", Servers));
        Pages.Add(new PageItem("Mod", Mods));
        Pages.Add(new PageItem("控制台", Console));

        _currentPage = Pages[0];
        _currentView = Auth;
    }

    partial void OnCurrentPageChanged(PageItem? value)
    {
        if (value is null) return;
        CurrentView = value.View;
        if (value.View is ModsPageViewModel m) m.Refresh();
        if (value.View is ConsolePageViewModel c) c.OnShown();
    }

    [RelayCommand]
    private void Navigate(PageItem page)
    {
        if (page == null) return;
        CurrentPage = page;
    }

    public void Dispose()
    {
        _transport.Dispose();
        if (Log.Logger is IDisposable d) d.Dispose();
    }
}

public sealed class PageItem
{
    public string Title { get; }
    public ViewModelBase View { get; }

    public PageItem(string title, ViewModelBase view)
    {
        Title = title;
        View = view;
    }
}
