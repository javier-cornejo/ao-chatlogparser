/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.cornejo.anarchyonline.multitool;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mx.cornejo.anarchyonline.multitool.plugins.Plugin;
import mx.cornejo.anarchyonline.multitool.plugins.backpacknamer.BackpackNamer;
import mx.cornejo.anarchyonline.multitool.plugins.logparser.LogParser;

import mx.cornejo.anarchyonline.utils.AOUtils;

/**
 *
 * @author javier
 */
public class MultiTool extends JFrame
{
    public static final String PREFS_AO_PREFS_DIR = "ao_prefs_dir";
    public static final String PREFS_LAST_USED_TAB = "last_used_tab";
    
    private static final Logger LOG = Logger.getLogger(MultiTool.class.getCanonicalName());
    private static final Preferences prefs = Preferences.userNodeForPackage(MultiTool.class);

    private ResourceBundle resourceBundle = null;
    private List<Plugin> plugins = null;
    
    public MultiTool()
    {
        super();
        
        Image icon = Toolkit.getDefaultToolkit().getImage(MultiTool.class.getResource("149947.gif"));
        setIconImage(icon);
        
        resourceBundle = ResourceBundle.getBundle("mx.cornejo.anarchyonline.multitool.Messages");        

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        checkSettings();
        setTitle(getString("app.title") + " - " + prefs.get(PREFS_AO_PREFS_DIR, ""));

        plugins = loadPlugins();
        buildGUI();
    }
    
    private String getString(String key)
    {
        return resourceBundle.getString(key);
    }
    
    private List<Plugin> loadPlugins()
    {
        List<Plugin> pluginList = new ArrayList();

        pluginList.add(new BackpackNamer());
        pluginList.add(new LogParser());
        
        pluginList.stream().forEach((plugin) -> 
        {
            plugin.setGlobalPreferences(prefs);
        });
        
        return pluginList;
    }

    private JMenuBar buildMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        
        {
            JMenu menu = new JMenu(getString("menu.file"));
            menu.setMnemonic(KeyEvent.VK_F);
            menu.getAccessibleContext().setAccessibleDescription(getString("menu.file.desc"));
            
            {
                JMenuItem menuItem = new JMenuItem(getString("menu.settings"));
                menuItem.setMnemonic(KeyEvent.VK_S);
                menuItem.getAccessibleContext().setAccessibleDescription(getString("menu.settings.desc"));
                menu.add(menuItem);
            }
            
            {
                JMenuItem menuItem = new JMenuItem(getString("menu.exit"));
                menuItem.setMnemonic(KeyEvent.VK_E);
                //menuItem.getAccessibleContext().setAccessibleDescription(getString("menu.exit.desc"));
                menuItem.addActionListener((e) ->
                {
                    System.exit(0);
                });
                menu.add(menuItem);
            }
            
            menuBar.add(menu);
        }
        
        {
            JMenu menu = new JMenu(getString("menu.help"));
            menu.setMnemonic(KeyEvent.VK_H);
            menu.getAccessibleContext().setAccessibleDescription(getString("menu.help.desc"));

            {
                JMenuItem menuItem = new JMenuItem(getString("menu.about"));
                menuItem.setMnemonic(KeyEvent.VK_A);
                menuItem.getAccessibleContext().setAccessibleDescription(getString("menu.about.desc"));
                menuItem.addActionListener((e) -> 
                {
                    
                });
                menu.add(menuItem);
            }

            menuBar.add(menu);
        }

        return menuBar;
    }
    
    private void buildGUI()
    {
        setSize(800,600);
        setJMenuBar(buildMenuBar());
        
        JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
        plugins.stream().forEach((plugin) ->
        {
            JPanel panel = plugin.getPanel();
            tabPane.add(panel.getName(), panel);
        });
        
        String lastTabUsed = prefs.get(PREFS_LAST_USED_TAB, null);
        if (lastTabUsed != null)
        {
            tabPane.setSelectedIndex(Integer.parseInt(lastTabUsed));
        }
        
        tabPane.addChangeListener((e) -> 
        {
            JTabbedPane tp = (JTabbedPane)e.getSource();
            int tabIdx = tp.getSelectedIndex();
            prefs.put(PREFS_LAST_USED_TAB, Integer.toString(tabIdx));
        });
        
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(tabPane, new GridBagConstraints(0,0, 1,1, 1.0,1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0,0));
    }
    
    private void checkSettings()
    {
        String aoPrefsPath = prefs.get(PREFS_AO_PREFS_DIR, null);
        
        // Try to guess the ao prefs dir
        if (aoPrefsPath == null)
        {
            File dir = AOUtils.getAOPrefsDir();
            if (dir != null && dir.exists())
            {
                aoPrefsPath = dir.getAbsolutePath();
            }
        }
        
        // Ask user to input the prefs dir
        if (aoPrefsPath == null)
        {
            int option = JOptionPane.showConfirmDialog(this, 
                        getString("dialog.no_prefs_dir.mssg"), 
                        getString("dialog.no_prefs_dir.title"), 
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
            if (option == JOptionPane.YES_OPTION)
            {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int returnVal = fc.showOpenDialog(MultiTool.this);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File selectedFile = fc.getSelectedFile();
                    if (selectedFile != null && selectedFile.isDirectory())
                    {
                        aoPrefsPath = selectedFile.getAbsolutePath();
                        // LOG.info("Selected: " + aoPrefsPath);
                    }
                }
            }    
        }
        
        prefs.put(PREFS_AO_PREFS_DIR, aoPrefsPath);
    }
    
    private void cleanUp()
    {
        plugins.stream().forEach((plugin) -> 
        {
            plugin.cleanUp();
        });
    }
    
    private void handleException(Exception ex, String sourceClass, String sourceMethod)
    {
        LOG.throwing(sourceClass, sourceMethod, ex);
        ex.printStackTrace();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException|
                InstantiationException|
                IllegalAccessException|
                UnsupportedLookAndFeelException ex)
        {
            ex.printStackTrace();
        }
        
        MultiTool multiTool = new MultiTool();
        
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                multiTool.cleanUp();
            }
        });
        
        multiTool.setVisible(true);
    }
}
