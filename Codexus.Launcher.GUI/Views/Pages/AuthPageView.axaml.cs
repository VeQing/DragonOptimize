using Avalonia.Controls;
using Avalonia.Markup.Xaml;

namespace Codexus.Launcher.GUI.Views.Pages;

public partial class AuthPageView : UserControl
{
    public AuthPageView()
    {
        InitializeComponent();
    }

    private void InitializeComponent()
    {
        AvaloniaXamlLoader.Load(this);
    }
}
