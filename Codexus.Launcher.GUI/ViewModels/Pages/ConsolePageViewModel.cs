using System.Collections.ObjectModel;
using Avalonia.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Codexus.Launcher.GUI.Services;

namespace Codexus.Launcher.GUI.ViewModels.Pages;

public partial class ConsolePageViewModel : ViewModelBase
{
    private readonly TransportService _transport;
    private readonly UiLogSink _log;

    public ReadOnlyObservableCollection<UiLogSink.LogEntry> LogItems => _log.Items;

    [ObservableProperty] private string _stateText = "空闲";
    [ObservableProperty] private bool _canStart = true;
    [ObservableProperty] private bool _canStop;

    public ConsolePageViewModel(TransportService transport, UiLogSink log)
    {
        _transport = transport;
        _log = log;
        _transport.StateChanged += OnTransportStateChanged;
    }

    private void OnTransportStateChanged(object? sender, TransportState s)
    {
        // 后台线程触发，切回 UI
        Dispatcher.UIThread.Post(() =>
        {
            StateText = s switch
            {
                TransportState.Idle => "空闲",
                TransportState.Connecting => "连接中...",
                TransportState.Connected => "已连接",
                TransportState.Failed => "失败",
                _ => s.ToString()
            };
            CanStart = s == TransportState.Idle || s == TransportState.Failed;
            CanStop = s == TransportState.Connecting || s == TransportState.Connected;
        });
    }

    public void OnShown()
    {
        // 简单触发一下属性刷新，让绑定更新
        OnPropertyChanged(nameof(LogItems));
    }

    [RelayCommand]
    private void Clear()
    {
        _log.Clear();
    }

    [RelayCommand]
    private void Stop()
    {
        _transport.Stop();
    }
}
