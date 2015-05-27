/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package org.opentravel.schemas.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.opentravel.ns.ota2.repositoryinfo_v01_00.RefreshPolicy;
import org.opentravel.schemacompiler.loader.LibraryLoaderException;
import org.opentravel.schemacompiler.loader.LibraryModelLoader;
import org.opentravel.schemacompiler.model.AbstractLibrary;
import org.opentravel.schemacompiler.repository.Project;
import org.opentravel.schemacompiler.repository.ProjectItem;
import org.opentravel.schemacompiler.repository.ProjectManager;
import org.opentravel.schemacompiler.repository.RepositoryException;
import org.opentravel.schemacompiler.repository.RepositoryItem;
import org.opentravel.schemacompiler.repository.RepositoryManager;
import org.opentravel.schemacompiler.repository.RepositoryNamespaceUtils;
import org.opentravel.schemacompiler.repository.impl.BuiltInProject;
import org.opentravel.schemacompiler.repository.impl.RemoteRepositoryClient;
import org.opentravel.schemacompiler.saver.LibrarySaveException;
import org.opentravel.schemacompiler.validate.ValidationFindings;
import org.opentravel.schemas.node.INode;
import org.opentravel.schemas.node.LibraryChainNode;
import org.opentravel.schemas.node.LibraryNode;
import org.opentravel.schemas.node.Node;
import org.opentravel.schemas.node.NodeNameUtils;
import org.opentravel.schemas.node.ProjectNode;
import org.opentravel.schemas.node.properties.ElementNode;
import org.opentravel.schemas.preferences.DefaultPreferences;
import org.opentravel.schemas.properties.Messages;
import org.opentravel.schemas.stl2developer.Activator;
import org.opentravel.schemas.stl2developer.DialogUserNotifier;
import org.opentravel.schemas.stl2developer.FileDialogs;
import org.opentravel.schemas.stl2developer.FindingsDialog;
import org.opentravel.schemas.stl2developer.OtmRegistry;
import org.opentravel.schemas.types.TypeResolver;
import org.opentravel.schemas.views.ValidationResultsView;
import org.opentravel.schemas.wizards.NewProjectValidator;
import org.opentravel.schemas.wizards.NewProjectWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Project Controller.
 * 
 * Reads/Saves open projects in session store. Creates built in project for built in libraries. Creates and manages
 * access to default project if not restored from session.
 * 
 * @author Dave Hollander
 * 
 */
public class DefaultProjectController implements ProjectController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProjectController.class);

	public static final String PROJECT_EXT = "otp";
	private final MainController mc;
	private final ProjectManager projectManager;
	protected ProjectNode defaultProject = null;
	protected ProjectNode builtInProject;

	private String defaultNS = "http://www.opentravel.org/OTM2/DefaultProject";
	private String defaultPath;

	// public String getDefaultPath() {
	// return defaultPath;
	// }

	// override with ns of local repository
	private final String dialogText = "Select a project file";

	/**
	 * Create controller and open projects in background thread.
	 */
	public DefaultProjectController(final MainController mc, RepositoryManager repositoryManager) {
		this.mc = mc;
		this.projectManager = new ProjectManager(mc.getModelController().getTLModel(), true, repositoryManager);

		// Find the Built-in project
		// for (Project p : projectManager.getAllProjects()) {
		// if (p.getProjectId().equals(BuiltInProject.BUILTIN_PROJECT_ID)) {
		// builtInProject = loadProject(p);
		// }
		// }

		final XMLMemento memento = getMemento();
		// If there is a display then run in the background.
		// Otherwise run in foreground as needed in junits.
		//
		if (memento == null) {
			loadSavedState(memento, null);
			syncWithUi("Projects opened.");

		} else {
			// Start the non-ui thread Job to do initial load of projects in background
			Job job = new Job("Opening Projects") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					loadSavedState(memento, monitor);
					syncWithUi("Projects opened.");
					return Status.OK_STATUS;
				}

			};
			job.setUser(false);
			job.schedule();
		}
		// LOGGER.info("Done initializing " + this.getClass());
	}

	public void syncWithUi(final String msg) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				// DialogUserNotifier.openInformation("Done ", msg);
				mc.postStatus("Done");
				mc.refresh();
			}
		});
	}

	protected void createDefaultProject() {
		defaultPath = DefaultPreferences.getDefaultProjectPath();
		defaultProject = create(new File(defaultPath), defaultNS, "Default Project",
				"Used for libraries not loaded into a project.");

		if (defaultProject == null) {
			DialogUserNotifier.openError("Default Project Error",
					Messages.getString("error.openProject.defaultProject", defaultPath));
		}
	}

	@Override
	public ProjectNode openProject(String fileName) {
		Project project = openProject(fileName, new ValidationFindings());
		return loadProject(project);
	}

	protected void fixElementNames(LibraryNode ln) {
		int fixNeeded = 0;
		if (!ln.isEditable())
			return;

		for (Node n : ln.getDescendants_TypeUsers()) {
			if (n instanceof ElementNode)
				if (!(n.getName().equals(NodeNameUtils.fixElementName(n))))
					fixNeeded++;
		}
		if (fixNeeded > 0) {
			if (DialogUserNotifier.openConfirm("Name Rules", fixNeeded
					+ " errors in element naming were detected. Should these be fixed automatically?"))
				for (Node n : ln.getDescendants_TypeUsers()) {
					if (n instanceof ElementNode)
						if (!(n.getName().equals(NodeNameUtils.fixElementName(n))))
							((ElementNode) n).setName("");
				}
		}
	}

	public List<ProjectItem> openLibrary(Project project, List<File> fileName, ValidationFindings findings)
			throws LibraryLoaderException, RepositoryException {
		List<ProjectItem> newItems = null;
		// LOGGER.debug("Adding to project: " + project.getName() + " library files: " + fileName);
		newItems = projectManager.addUnmanagedProjectItems(fileName, project, findings);
		return newItems;
	}

	/**
	 * Does NOT add them to the Node project model. Use {projectNode}.add(FileList)
	 */
	@Override
	public List<ProjectItem> addLibrariesToTLProject(Project project, List<File> libraryFiles) {
		ValidationFindings findings = new ValidationFindings();
		List<ProjectItem> newItems = Collections.emptyList();
		try {
			newItems = openLibrary(project, libraryFiles, findings);
			ValidationFindings loadingFindings = getLoadingFindings(findings);
			if (!loadingFindings.isEmpty()) {
				showFindings(loadingFindings);
			}
		} catch (RepositoryException e) {
			LOGGER.error("Could not add library file to project.");
			DialogUserNotifier.openError("Project Error", "Could not add to project.");
		} catch (LibraryLoaderException e) {
			LOGGER.error("Could not add library file to project.");
			DialogUserNotifier.openError("Project Error", "Could not add to project.");
		} catch (Throwable e) {
			LOGGER.error("Error when adding to project", e);
			DialogUserNotifier.openError("Project Error", "Could not add to project.");
		}
		// LOGGER.debug("Added libraries to " + project.getName());
		return newItems;
	}

	/**
	 * @param loadingErros
	 */
	private void showFindings(final ValidationFindings loadingErros) {
		// do not block UI
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				FindingsDialog.open(OtmRegistry.getActiveShell(), Messages.getString("dialog.findings.title"),
						Messages.getString("dialog.findings.message"), loadingErros.getAllFindingsAsList());

			}
		});
	}

	@Override
	public LibraryNode add(ProjectNode pn, AbstractLibrary tlLib) {
		if (pn == null || tlLib == null)
			throw new IllegalArgumentException("Null argument.");

		ProjectItem pi = null;
		LibraryNode ln = null;
		try {
			pi = pn.getProject().getProjectManager().addUnmanagedProjectItem(tlLib, pn.getProject());
			if (pi != null) {
				ln = new LibraryNode(pi, pn);
				fixElementNames(ln);
				mc.refresh(pn);
				// LOGGER.debug("Added library " + ln.getName() + " to " + pn);
			}
		} catch (RepositoryException e) {
			LOGGER.error("Could not add repository item to project. " + e.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", e.getLocalizedMessage());
		} catch (IllegalArgumentException ex) {
			LOGGER.error("Could not add repository item to project. " + ex.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", ex.getLocalizedMessage());
		}
		return ln;
	}

	@Override
	public LibraryNode add(LibraryNode ln, AbstractLibrary tlLib) {
		ProjectNode pn = ln.getProject();
		if (pn == null || tlLib == null)
			throw new IllegalArgumentException("Null argument.");

		ProjectItem pi = null;
		try {
			pi = pn.getProject().getProjectManager().addUnmanagedProjectItem(tlLib, pn.getProject());
		} catch (RepositoryException e) {
			LOGGER.error("Could not add repository item to project. " + e.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", e.getLocalizedMessage());
		}
		if (pi != null)
			ln = new LibraryNode(pi, ln.getChain());
		mc.refresh(pn);
		// LOGGER.debug("Added library " + ln.getName() + " to " + pn);
		return ln;
	}

	/**
	 * Add the modeled library to the project.
	 * 
	 * @param pn
	 * @param ln
	 */
	public ProjectItem add(ProjectNode pn, LibraryNode ln) {
		if (pn == null || ln == null)
			return null;

		ProjectItem pi = null;
		try {
			pi = pn.getProject().getProjectManager().addUnmanagedProjectItem(ln.getTLaLib(), pn.getProject());
		} catch (RepositoryException e) {
			LOGGER.error("Could not add repository item to project. " + e.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", e.getLocalizedMessage());
		}
		ln.setProjectItem(pi);
		return pi;
	}

	@Override
	public ProjectItem add(ProjectNode project, RepositoryItem ri) {
		if (project == null)
			return null;

		ProjectItem pi = null;
		List<ProjectItem> piList = null;
		ValidationFindings findings = new ValidationFindings();
		try {
			// workaround to make sure the local version of library will be up-to-date with remote
			// repository
			RefreshPolicy refreshPolicy = null;
			if (ri.getRepository() instanceof RemoteRepositoryClient) {
				RemoteRepositoryClient client = (RemoteRepositoryClient) ri.getRepository();
				refreshPolicy = client.getRefreshPolicy();
				client.setRefreshPolicy(RefreshPolicy.ALWAYS);
			}
			piList = project.getProject().getProjectManager()
					.addManagedProjectItems(Arrays.asList(new RepositoryItem[] { ri }), project.getProject(), findings);

			// restore refresh policy
			if (ri.getRepository() instanceof RemoteRepositoryClient) {
				RemoteRepositoryClient client = (RemoteRepositoryClient) ri.getRepository();
				client.setRefreshPolicy(refreshPolicy);
			}
			if (findings.hasFinding()) {
				if (PlatformUI.isWorkbenchRunning())
					FindingsDialog.open(OtmRegistry.getActiveShell(), Messages.getString("dialog.findings.title"),
							Messages.getString("dialog.findings.message"), findings.getAllFindingsAsList());
			}
			if (!piList.isEmpty())
				pi = piList.get(0);
		} catch (LibraryLoaderException e) {
			LOGGER.error("Could not add repository item to project. " + e.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", e.getLocalizedMessage());

		} catch (RepositoryException e) {
			LOGGER.error("Could not add repository item to project. " + e.getLocalizedMessage());
			DialogUserNotifier.openError("Project Error", e.getLocalizedMessage());
		}

		// TEST - make sure any member of the chain is not already opened.
		// piList now has all of the project items for this chain.
		List<LibraryChainNode> chains = new ArrayList<LibraryChainNode>();
		if (!piList.isEmpty()) {
			for (ProjectItem item : piList) {
				if (pi == null)
					pi = item; // return first one
				if (item != null) {
					LibraryChainNode lcn = project.getChain(item);
					// FIXME - getChain is always returning null causing each chain member to create
					// a new chain.
					if (lcn == null) {
						lcn = new LibraryChainNode(item, project);
						chains.add(lcn);
					} else {
						lcn.add(item);
					}
				}

				try {
					project.getProject().getProjectManager().saveProject(project.getProject());
				} catch (LibrarySaveException e) {
					e.printStackTrace();
					DialogUserNotifier.openError("Could not save project.", e.getLocalizedMessage());
				}
			}
			TypeResolver tr = new TypeResolver();
			tr.resolveTypes(); // do the whole model to check if new libraries resolved broken
								// links.
			// for (LibraryChainNode chain : chains) {
			// tr.resolveTypes(chain.getLibraries());
			// }
			mc.refresh(project);
		} else
			LOGGER.warn("Repository item " + ri.getLibraryName() + " not added to project.");

		// TODO - sync the repository view
		// LOGGER.debug("Added repository item " + ri.getLibraryName() + " to " + project);
		return pi;
	}

	@Override
	public ProjectNode create(File file, String ID, String name, String description) {
		Project newProject;
		try {
			newProject = projectManager.newProject(file, ID, name, description);
		} catch (Exception e) {
			LOGGER.error("Could not create new project: ", e);
			DialogUserNotifier.openError("New Project Error", e.getLocalizedMessage());
			return null;
		}
		return new ProjectNode(newProject);
		// TODO - what to do if the default project is corrupt. The user will have no way to fix it.
	}

	@Override
	public List<ProjectNode> getAll() {
		List<ProjectNode> projects = new ArrayList<ProjectNode>();
		for (INode n : mc.getModelNode().getChildren()) {
			if (n instanceof ProjectNode)
				projects.add((ProjectNode) n);
		}
		return projects;
	}

	/**
	 * @return the builtInProject
	 */
	@Override
	public ProjectNode getBuiltInProject() {
		return builtInProject;
	}

	/**
	 * @return the defaultProject
	 */
	@Override
	public ProjectNode getDefaultProject() {
		// if (defaultProject == null)
		// createDefaultProject();
		return defaultProject;
	}

	/**
	 * @return the namespace managed by the default project.
	 */
	@Override
	public String getDefaultUnmanagedNS() {
		return defaultProject == null ? "" : defaultProject.getNamespace();
	}

	/**
	 * @param defaultProject
	 *            the defaultProject to set
	 */
	public void setDefaultProject(ProjectNode defaultProject) {
		this.defaultProject = defaultProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentravel.schemas.controllers.ProjectController#getNamespace()
	 */
	@Override
	public String getNamespace() {
		return null;
	}

	@Override
	public void open() {
		String[] extensions = { "*." + PROJECT_EXT };
		final String fn = FileDialogs.postFileDialog(extensions, dialogText);
		if (fn == null)
			return;

		if (Display.getCurrent() == null)
			open(fn, null); // not in UI Thread
		else {
			// run in a background job
			mc.postStatus("Opening " + fn);
			Job job = new Job("Opening Projects") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Opening Project: " + fn, 3);
					monitor.worked(1);
					open(fn, monitor);
					monitor.done();
					syncWithUi("Project Opened");
					return Status.OK_STATUS;
				}
			};
			job.setUser(true);
			job.schedule();
		}
	}

	// private void syncWithUi() {
	// Display.getDefault().asyncExec(new Runnable() {
	// public void run() {
	// MessageDialog.openInformation(mainWindow.getSite().getShell(), "Done ", "Projects opened.");
	// refresh();
	// }
	// });
	// }
	// }

	public Project openProject(String fileName, ValidationFindings findings) {
		// LOGGER.debug("Opening Project. Filename = " + fileName);
		File projectFile = new File(fileName);
		Project project = null;
		boolean isUI = Display.getCurrent() != null;
		if (isUI)
			mc.showBusy(true);
		try {
			project = projectManager.loadProject(projectFile, findings);
		} catch (RepositoryException e) {
			if (isUI)
				mc.showBusy(false);
			if (isUI)
				DialogUserNotifier.openError(
						"Project Error",
						MessageFormat.format(Messages.getString("error.openProject.invalidRemoteProject"),
								e.getMessage(), projectFile.toString()));
		} catch (LibraryLoaderException e) {
			// happens when default project is created.
			if (isUI) {
				mc.showBusy(false);
				DialogUserNotifier.openError("Project Error", "Could not load project libraries.");
			}
			project = null;
		} catch (IllegalArgumentException e) {
			if (isUI)
				mc.showBusy(false);
			if (isUI)
				DialogUserNotifier.openError("Error opening project. ", e.getMessage());
			project = null;
		} catch (Throwable e) {
			if (isUI) {
				mc.showBusy(false);
				String msg = e.getMessage();
				if (msg == null || msg.isEmpty())
					msg = e.getClass().getSimpleName();
				DialogUserNotifier.openError("Error opening project. ", msg);
			}
			project = null;
		}
		if (isUI)
			mc.showBusy(false);
		return project;
	}

	public ProjectNode open(String fileName, IProgressMonitor monitor) {
		if (fileName == null || fileName.isEmpty())
			LOGGER.error("Tried to open null or empty file.");
		LOGGER.debug("Opening project from file: " + fileName);

		ValidationFindings findings = null;
		Project project = openProject(fileName, findings);
		if (monitor != null)
			monitor.worked(1);
		ProjectNode pn = loadProject(project);

		return pn;
	}

	/**
	 * @param findings
	 * @return
	 */
	private ValidationFindings getLoadingFindings(ValidationFindings findings) {
		return LibraryModelLoader.filterLoaderFindings(findings);
	}

	/**
	 * Load a TL project into the GUI model. Creates Project Node which adds all the libraries in the project.
	 * 
	 * @param project
	 */
	public ProjectNode loadProject(Project project) {
		if (project == null)
			return null;
		if (Display.getCurrent() != null) {
			mc.showBusy(true);
			mc.postStatus("Loading Project: " + project.getName());
			mc.refresh();
		}
		LOGGER.debug("Creating Project Node.");
		ProjectNode pn = new ProjectNode(project);
		for (LibraryNode ln : pn.getLibraries())
			fixElementNames(ln);
		if (Display.getCurrent() != null) {
			mc.selectNavigatorNodeAndRefresh(pn);
			mc.postStatus("Loaded Project: " + pn);
			mc.showBusy(false);
		}
		return pn;
	}

	/**
	 * Load the built-in project. Reads tl project from project manager.
	 */
	public void loadProject_BuiltIn() {
		for (Project p : projectManager.getAllProjects()) {
			if (p.getProjectId().equals(BuiltInProject.BUILTIN_PROJECT_ID)) {
				builtInProject = loadProject(p);
			}
		}
		Job job = new Job("Opening Projects") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Opening Project: ", 3);
				monitor.worked(1);
				// open(fn, monitor);
				monitor.done();
				syncWithUi("Project Opened");
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

	}

	@Override
	public void close(ProjectNode pn) {
		if (pn == null || pn.isBuiltIn()) {
			return;
		}

		save(pn);
		pn.getProject().getProjectManager().closeProject(pn.getProject());
		pn.close();
		// reload default project
		if (pn == getDefaultProject()) {
			ProjectNode newDefaultProject = openProject(getDefaultProject().getProject().getProjectFile().toString());
			defaultProject = newDefaultProject;
		}
		mc.refresh();
		// LOGGER.debug("Closed project: " + pn);
	}

	@Override
	public void closeAll() {
		for (ProjectNode project : getAll()) {
			close(project);
		}
		// to re-initializes the contents of the model
		projectManager.closeAll();
	}

	@Override
	public void save() {
		save(getDefaultProject());
	}

	@Override
	public void save(ProjectNode pn) {
		if (pn == null)
			return;

		mc.showBusy(true);
		try {
			pn.getProject().getProjectManager().saveProject(pn.getProject());
			// save the project file only, not the libraries and ignore findings
			// pn.getProject().getProjectManager().saveProject(pn.getProject(), true, null);
		} catch (LibrarySaveException e) {
			// e.printStackTrace();
			mc.showBusy(false);
			LOGGER.error("Could not save project");
			DialogUserNotifier.openError("Project Error", "Could not save project.");
		}
		mc.showBusy(false);
	}

	@Override
	public void save(List<ProjectNode> projects) {
		// UNUSED
		for (ProjectNode pn : projects)
			save(pn);
	}

	@Override
	public void saveAll() {
		// UNUSED
		ProjectManager pm = getDefaultProject().getProject().getProjectManager();
		for (Project p : pm.getAllProjects())
			try {
				pm.saveProject(p);
			} catch (LibrarySaveException e) {
				e.printStackTrace();
				LOGGER.error("Could not save project");
			}

	}

	@Override
	public ProjectNode newProject() {
		// Run the wizard
		final NewProjectWizard wizard = new NewProjectWizard();
		wizard.setValidator(new NewProjectValidator());
		wizard.run(OtmRegistry.getActiveShell());
		if (!wizard.wasCanceled()) {
			create(wizard.getFile(), wizard.getNamespace(), wizard.getName(), wizard.getDescription());
			mc.refresh();
		}
		return null;
	}

	@Override
	public void newProject(String defaultName, String selectedRoot, String selectedExt) {
		// Run the wizard
		final NewProjectWizard wizard = new NewProjectWizard(defaultName, selectedRoot, selectedExt);
		wizard.setValidator(new NewProjectValidator());
		wizard.run(OtmRegistry.getActiveShell());
		if (!wizard.wasCanceled()) {
			create(wizard.getFile(), wizard.getNamespace(), wizard.getName(), wizard.getDescription());
			mc.refresh();
		}
	}

	@Override
	public List<String> getSuggestedNamespaces() {
		List<String> allowedNSs = new ArrayList<String>();
		allowedNSs.add(getDefaultUnmanagedNS());
		allowedNSs.addAll(getOpenGovernedNamespaces());
		return allowedNSs;
	}

	@Override
	public List<String> getOpenGovernedNamespaces() {
		List<String> projects = new ArrayList<String>();
		for (Project p : projectManager.getAllProjects()) {
			if (p instanceof BuiltInProject) {
				continue;
			}
			projects.add(RepositoryNamespaceUtils.normalizeUri(p.getProjectId()));
		}
		return projects;
	}

	/**
	 * ** Manage Saving/Retrieving State
	 * 
	 * TODO - refactor into its own class/file
	 */

	// private boolean loadSavedState() {
	// if (!OtmRegistry.getMainWindow().hasDisplay())
	// return false;
	//
	// FileReader reader = null;
	// try {
	// reader = new FileReader(getOTM_StateFile());
	// loadSavedState(XMLMemento.createReadRoot(reader));
	// } catch (FileNotFoundException e) {
	// // Ignored... no items exist yet.
	// return false;
	// } catch (Exception e) {
	// // Log the exception and move on.
	// LOGGER.error("LoadSavedState: " + getOTM_StateFile().toString() + " e= " + e);
	// return false;
	// } finally {
	// try {
	// if (reader != null)
	// reader.close();
	// } catch (IOException e) {
	// LOGGER.error("LoadState: " + e);
	// return false;
	// }
	// }
	// return true;
	// }

	/**
	 * Get the memento with project data in them. Must be done in UI thread.
	 * 
	 * @return
	 */
	public XMLMemento getMemento() {
		if (Display.getCurrent() == null)
			LOGGER.warn("Warning - getting memento from thread that is not UI thread.");
		XMLMemento memento = null;
		FileReader reader = null;
		try {
			reader = new FileReader(getOTM_StateFile());
			memento = XMLMemento.createReadRoot(reader);
		} catch (FileNotFoundException e) {
			// Ignored... no items exist yet.
			return null;
		} catch (Exception e) {
			// Log the exception and move on.
			LOGGER.error("getMemento error: " + getOTM_StateFile().toString() + " e= " + e);
			return null;
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				LOGGER.error("getMemento error: " + e);
				return null;
			}
		}
		// printout projects
		IMemento[] children = memento.getChildren(OTM_PROJECT);
		for (int i = 0; i < children.length; i++) {
			LOGGER.debug("Memento project: " + children[i].getString(OTM_PPOJECT_LOCATION));
		}

		return memento;
	}

	public void loadSavedState(XMLMemento memento, IProgressMonitor monitor) {
		if (memento != null) {
			IMemento[] children = memento.getChildren(OTM_PROJECT);
			monitor.beginTask("Opening Projects", memento.getChildren(OTM_PROJECT).length * 2);

			for (int i = 0; i < children.length; i++) {
				monitor.subTask("Opening Project: " + children[i].getString(OTM_PPOJECT_LOCATION));
				open(children[i].getString(OTM_PPOJECT_LOCATION), monitor);
				monitor.worked(1);
				if (monitor.isCanceled())
					break;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						mc.refresh(); // update the user interface asynchronously
					}
				});
			}
		}
		defaultNS = OtmRegistry.getMainController().getRepositoryController().getLocalRepository().getNamespace();
		for (ProjectNode pn : getAll()) {
			if (pn.getProject().getProjectId().equals(defaultNS)) {
				defaultProject = pn;
				break;
			}
		}
		if (defaultProject == null) {
			createDefaultProject();
		}
	}

	Collection<IProjectToken> openProjects = new ArrayList<DefaultProjectController.IProjectToken>();
	private static final String OTM_PROJECTS = "OTM_Projects";
	// private static final String TAG_FAVORITES = "Favorites";
	public static final String OTM_PROJECT = "Project";
	private static final String OTM_PROJECT_NAME = "Name";
	private static final String OTM_PPOJECT_LOCATION = "Location";

	/**
	 * Save the currently open project state using the Eclipse utilities. Saved to:
	 * <workspace>/.metadata/.plugins/org.opentravel...
	 */
	@Override
	public void saveState() {
		List<ProjectNode> projects = getAll();
		// if (projects.isEmpty()) return;
		for (ProjectNode p : projects) {
			// Filter out built-in and default project?
			// Make into Items
			if (p.getProject().getProjectFile() != null) {
				IProjectToken item = new ProjectToken(p.getProject().getProjectFile());
				openProjects.add(item);
			}
		}
		// LOGGER.debug("Made project list into project tokens.");

		XMLMemento memento = XMLMemento.createWriteRoot(OTM_PROJECTS);
		saveFavorites(memento);
		FileWriter writer = null;
		try {
			writer = new FileWriter(getOTM_StateFile());
			memento.save(writer);
		} catch (IOException e) {
			LOGGER.error("IO Error saving state.");
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				LOGGER.error("IO Error closing state file.");
			}
		}
		// try {
		// LOGGER.debug("Saved project state to file: " + getOTM_StateFile().getCanonicalPath());
		// } catch (IOException e) {
		// }
	}

	private File getOTM_StateFile() {
		return Activator.getDefault().getStateLocation().append("OTM_Developer.xml").toFile();
	}

	private void saveFavorites(XMLMemento memento) {
		Iterator<IProjectToken> iter = openProjects.iterator();
		while (iter.hasNext()) {
			IProjectToken item = iter.next();
			IMemento child = memento.createChild(OTM_PROJECT);
			child.putString(OTM_PROJECT_NAME, item.getName());
			child.putString(OTM_PPOJECT_LOCATION, item.getLocation());
		}
	}

	// TODO - eliminate this extra class - just load into the project
	public interface IProjectToken extends IAdaptable {
		String getName();

		void setName(String newName);

		String getLocation();

		// boolean isFavoriteFor(Object obj);
		// FavoriteItemType getType();
		// String getInfo();
		static IProjectToken[] NONE = new IProjectToken[] {};
	}

	public class ProjectToken implements IProjectToken {
		private String name;
		private String location;

		// private final String id;
		// private final int ordinal;

		public ProjectToken(File file) {
			try {
				location = file.getCanonicalPath();
			} catch (IOException e) {
				LOGGER.error("Could not create project token due to bad path.");
				// e.printStackTrace();
			}
		}

		@Override
		public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String newName) {
			name = newName;
		}

		@Override
		public String getLocation() {
			return location;
		}

	}

	/**
	 * Open a project in a UI thread with wait cursor.
	 * 
	 * See DefaultRepositoryController for details.
	 * 
	 * @author Dave
	 * 
	 */
	class OpenProjectThread extends Thread {
		private String fileName;
		private ValidationFindings findings = new ValidationFindings();
		private ProjectNode projectNode = null;

		public OpenProjectThread(String filename) {
			this.fileName = filename;
		}

		public ValidationFindings getFindings() {
			return findings;
		}

		public ProjectNode getProjectNode() {
			return projectNode;
		}

		public void run() {
			Project project = openProject(fileName, findings);
			if (project != null) {
				ValidationFindings loadingFindings = getLoadingFindings(findings);
				if (!loadingFindings.isEmpty()) {
					showFindings(loadingFindings);
					// for (String finding : loadingFindings
					// .getAllValidationMessages(FindingMessageFormat.MESSAGE_ONLY_FORMAT))
					// LOGGER.debug("Finding: " + finding);
				}

				projectNode = loadProject(project); // Create gui model for the project

				final ValidationResultsView view = OtmRegistry.getValidationResultsView();
				if (view != null) {
					view.setFindings(findings, projectNode);
				}
			}
		}
	}

	// dead code. Moved to repository Controller.
	// public void versionLibrary(Node node) {
	// TODO - handle cases where there is no library chain .. start versioning
	// this lib
	// LOGGER.debug("Versioning Library " + node);
	// LibraryNode lib = null;
	// LibraryNode newLib = null;
	// ProjectNode project = null;

	// if (node instanceof LibraryChainNode) {
	// DEAD CODE - handler does a getLibrary();
	// LibraryChainNode lc = (LibraryChainNode) node;
	// lib = lc.getLibrary();
	// newLib = lc.incrementMinorVersion();
	// project = lib.getProject();
	// lc.getVersions().linkLibrary(newLib);
	// } else if (node instanceof LibraryNode) {
	// lib = (LibraryNode) node;
	// newLib = lib.createNewMinorVerison(lib.getProject());
	// project = lib.getProject();
	// lib.getParent().linkLibrary(newLib);
	// // FIXME - update containing chain head.
	// } else
	// LOGGER.error("Can't version Library " + node);
	//
	// if (newLib != null) {
	// // add(project, newLib);
	// if (lib.getProjectItem().getRepository() != null) {
	// try {
	// lib.getProjectItem().getProjectManager()
	// .publish(newLib.getProjectItem(), lib.getProjectItem().getRepository());
	// } catch (RepositoryException e) {
	// LOGGER.debug("Error publishing new version of library " + lib);
	// }
	// }
	// // TODO - is the project file updated correctly???
	// // NO. both the unmanaged and managed are added.
	// // TEST - Trying the add here instead of in front of publish
	// add(project, newLib);

	// }
	// FIXME - do not know what repo to sync.
	// mc.getRepositoryController().sync(null);
	// }

	// public void finalizeLibrary(LibraryNode lib) {
	// LOGGER.debug("Finalizing Library " + lib);
	//
	// try {
	// lib.getProjectItem().getProjectManager().promote(lib.getProjectItem());
	// } catch (RepositoryException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

}
