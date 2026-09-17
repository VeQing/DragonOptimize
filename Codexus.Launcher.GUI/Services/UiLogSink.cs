using System.Collections.ObjectModel;
using Avalonia;
using Avalonia.Threading;
using Serilog.Core;
using Serilog.Events;

namespace Codexus.Launcher.GUI.Services;

/// <summary>
/// 一个简单的内存日志桥：底层 Serilog / EventBus 写入的日志会进入这里，
/// 然后 UI 通过 ObservableCollection 绑定显示。容量有限，超出会丢弃最早的。
/// 写入自动切到 Avalonia UI 线程，避免后台线程碰 ObservableCollection。
/// </summary>
public sealed class UiLogSink : ILogEventSink
{
    public const int MaxLines = 2000;

    private readonly ObservableCollection<LogEntry> _items = new();
    public ReadOnlyObservableCollection<LogEntry> Items { get; }

    public UiLogSink()
    {
        Items = new ReadOnlyObservableCollection<LogEntry>(_items);
    }

    public void Emit(LogEvent logEvent)
    {
        // 后台线程会进这里，Post 到 UI 线程再写集合
        var level = logEvent.Level.ToString();
        var msg = logEvent.RenderMessage();
        var exStr = logEvent.Exception?.ToString();
        DispatchOnUiThread(() => Post(level, msg, exStr));
    }

    private static void DispatchOnUiThread(Action action)
    {
        // Avalonia 还没启动时直接同步执行；启动后用 UI 线程 Dispatcher
        if (Application.Current?.ApplicationLifetime is null)
        {
            action();
            return;
        }
        Avalonia.Threading.Dispatcher.UIThread.Post(action);
    }

    public void Post(string level, string message, string? exceptionText = null)
    {
        if (_items.Count >= MaxLines)
        {
            _items.RemoveAt(0);
        }
        _items.Add(new LogEntry(DateTime.Now, level, message, exceptionText));
    }

    /// <summary>重载：直接接收 Exception，方便 EventBus 后台线程调用。</summary>
    public void Post(string level, string message, Exception? ex)
        => Post(level, message, ex?.ToString());

    public void Clear() => _items.Clear();

    public readonly record struct LogEntry(DateTime Time, string Level, string Message, string? Exception);
}
