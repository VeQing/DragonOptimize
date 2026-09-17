using System.IO;
using System.Linq;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using Avalonia.Platform.Storage;
using Codexus.Launcher.GUI.ViewModels.Pages;

namespace Codexus.Launcher.GUI.Views.Pages;

public partial class ModsPageView : UserControl
{
    public ModsPageView()
    {
        InitializeComponent();
        AddButton.Click += OnAddClick;
    }

    private void InitializeComponent()
    {
        AvaloniaXamlLoader.Load(this);
    }

    private async void OnAddClick(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        if (DataContext is not ModsPageViewModel vm) return;
        var top = TopLevel.GetTopLevel(this);
        if (top is null) return;

        var files = await top.StorageProvider.OpenFilePickerAsync(new FilePickerOpenOptions
        {
            Title = "选择 Mod dll",
            AllowMultiple = true,
            FileTypeFilter = new[]
            {
                new FilePickerFileType("Mod dll") { Patterns = new[] { "*.dll" } },
                new FilePickerFileType("All files") { Patterns = new[] { "*.*" } }
            }
        });

        foreach (var f in files)
        {
            try
            {
                var path = f.TryGetLocalPath();
                if (!string.IsNullOrEmpty(path) && File.Exists(path))
                    vm.AddFromFile(path);
            }
            catch { /* ignore per-file error */ }
        }

        vm.Refresh();
    }
}
