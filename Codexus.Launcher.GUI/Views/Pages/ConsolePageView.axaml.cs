using Avalonia.Controls;
using Avalonia.Markup.Xaml;

namespace Codexus.Launcher.GUI.Views.Pages;

public partial class ConsolePageView : UserControl
{
    public ConsolePageView()
    {
        InitializeComponent();
    }

    private void InitializeComponent()
    {
        AvaloniaXamlLoader.Load(this);
    }
}
