package org.incha.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.commons.logging.LogFactory;
import org.incha.core.JavaProject;
import org.incha.core.JavaProjectsModel;
import org.incha.core.StatisticsManager;
import org.incha.ui.stats.ShowCurrentStateAction;
import org.incha.ui.stats.StartAnalysisAction;

public class JSwingRipplesApplication extends JFrame {
    private static final long serialVersionUID = 6142679404175274529L;

    /**
     * The view area
     */
    private final JDesktopPane viewArea = new JDesktopPane();
    private final ProjectsView projectsView;
    private static JSwingRipplesApplication instance;
    private final ProgressMonitorImpl progressMonitor = new ProgressMonitorImpl();

    /**
     * Default constructor.
     */
    public JSwingRipplesApplication() {
        super("JSwingRipples");
        instance = this;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final JPanel contentPane = new JPanel(new BorderLayout(0, 5));
        setContentPane(contentPane);
        contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));

        setJMenuBar(createMenuBar());

        projectsView = new ProjectsView(JavaProjectsModel.getInstance());
        projectsView.addProjectsViewMouseListener(new ProjectsViewMouseListener() {
            @Override
            public void handle(final ProjectsViewMouseEvent e) {
                handleProjectsViewMouseEvent(e);
            }
        });

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectsView, viewArea);
        getContentPane().add(pane, BorderLayout.CENTER);

        //add progress monitor.
        getContentPane().add(progressMonitor, BorderLayout.SOUTH);

        //init liteners
        final DefaultController controller = new DefaultController();
        StatisticsManager.getInstance().addStatisticsChangeListener(controller);


        //create model saver, this class will watch for model
        //and save it when model state changed
        new ModelSaver(JavaProjectsModel.getInstance(), JavaProjectsModel.getModelFile());
    }

    /**
     * @param e
     */
    protected void handleProjectsViewMouseEvent(final ProjectsViewMouseEvent e) {
        if (e.getType() != ProjectsViewMouseEvent.LEFT_MOUSE_PRESSED) {
            return;
        }

        final Object[] path = e.getPath();
        if (path[path.length -1] instanceof JavaProject) {
            final JavaProject project = (JavaProject) path[path.length -1];
            final JPopupMenu menu = new JPopupMenu();

            //delete project menu item
            final JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    JavaProjectsModel.getInstance().deleteProject(project);
                }
            });
            menu.add(delete);

            //project preferences menu item
            final JMenuItem prefs = new JMenuItem("Preferences");
            prefs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    showProjectSettinsEditor(project);
                }
            });
            menu.add(prefs);

            //start analisics
            final JMenuItem startAnalysis = new JMenuItem("Start analysis");
            startAnalysis.addActionListener(new StartAnalysisAction());
            menu.add(startAnalysis);

            menu.show(projectsView, e.getX(), e.getY());
        }
    }
    /**
     * @param project
     */
    protected void showProjectSettinsEditor(final JavaProject project) {
        final JFrame f = new JFrame("Project Settings");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().setLayout(new BorderLayout(0, 5));

        final ProjectSettingsEditor view = new ProjectSettingsEditor(project);
        f.getContentPane().add(view, BorderLayout.CENTER);

        //add ok button
        final JPanel south = new JPanel(new FlowLayout());
        final JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                f.dispose();
            }
        });
        south.add(ok);
        f.getContentPane().add(south, BorderLayout.SOUTH);

        //set frame location
        final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(size.width / 2, size.height / 2);
        f.setLocationRelativeTo(this);

        //show frame
        f.setVisible(true);
    }

    /**
     * @return
     */
    private JMenuBar createMenuBar() {
        final JMenuBar bar = new JMenuBar();

        //file menu
        final JMenu file = new JMenu("File");
        bar.add(file);

        //New Project option.
        final JMenuItem newProject = new JMenuItem("New Project");
        newProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                createNewProject();
            }
        });
        file.add(newProject);

        //Import Project option.
        final JMenuItem importProject = new JMenuItem("Import Project");
        importProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                importProject();
            }
        });
        file.add(importProject);

        //JRipples menu
        final JMenu jRipples = new JMenu("JRipples");
        bar.add(jRipples);

        final JMenuItem startAnalysis = new JMenuItem("Start analysis");
        startAnalysis.addActionListener(new StartAnalysisAction());
        jRipples.add(startAnalysis);

        jRipples.add(new JSeparator(JSeparator.HORIZONTAL));
        final JMenuItem currentState = new JMenuItem("Current state - statistics");
        currentState.addActionListener(new ShowCurrentStateAction());
        jRipples.add(currentState);
//        final JMenuItem manageStates = new JMenuItem("Manage Statess");
//        jRipples.add(manageStates);
//        final JMenuItem saveState = new JMenuItem("Save State");
//        jRipples.add(saveState);
//        final JMenuItem loadState = new JMenuItem("Load State");
//        jRipples.add(loadState);

        return bar;
    }

    /**
     * Creates new project.
     */
    protected void createNewProject() {
        final JavaProject project = NewProjectWizard.showDialog(this);
        if (project != null) {
            JavaProjectsModel.getInstance().addProject(project);
        }
    }

    /**
     * Import a project from a path.
     */
    protected void importProject(){
        final JavaProject project = NewProjectWizard.showDialog(this);
        if (project != null) {
            if(JavaProjectsModel.getInstance().addProject(project)) {
                new ImportSource(project);
            }
        }


    }
    /**
     * @return the application home folder.
     */
    public static File getHome() {
        return new File(System.getProperty("user.home") + File.separator + ".jswingripples");
    }

    public static void main(final String[] args) {
        //init logging
        getHome().mkdirs();

        final JFrame f = new JSwingRipplesApplication();

        //set frame location
        final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(size.width / 2, size.height / 2);
        f.setLocationByPlatform(true);
        LogFactory.getLog(JSwingRipplesApplication.class).debug("Prueba uno");
        
        // If called with the protocol args
        processArgs(args);
        

        //show frame
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                f.setVisible(true);
            }
        });
    }
    
    private static void processArgs(final String[] args) {
    	if (args.length != 0) {
    		JavaProjectsModel javaProjectsModel = JavaProjectsModel.getInstance();
    		JavaProject project = javaProjectsModel.getProjectByName(args[0]);
    		if (project == null) {
    			project = new JavaProject(args[0]);
    			javaProjectsModel.addProject(project);
    		}
    		for (int argNumber = 1; argNumber < args.length; argNumber++) {
    			project.getBuildPath().addSource(new File(args[argNumber]));
    		}
    	}
    }

    /**
     * Get function using singleton.
     * @return shared application window.
     */
    public static JSwingRipplesApplication getInstance() {
    	if(instance==null){
    		instance=new JSwingRipplesApplication();
    	}
        return instance;
    }

    /**
     * @return progress monitor.
     */
    public TaskProgressMonitor getProgressMonitor() {
        return this.progressMonitor;
    }
    /**
     * @return the viewArea
     */
    public JDesktopPane getViewArea() {
        return viewArea;
    }
}
