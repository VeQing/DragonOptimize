using Avalonia.Controls;
using Avalonia.Markup.Xaml;

namespace Codexus.Launcher.GUI.Views.Pages;

public partial class ServersPageView : UserControl
{
    public ServersPageView()
    {
        InitializeComponent();
    }

    private void InitializeComponent()
    {
        AvaloniaXamlLoader.Load(this);
    }
}
