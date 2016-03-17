package de.htwds.objectmacroplugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.project.MavenProjectHelper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultMavenProjectHelper;
import org.apache.maven.project.MavenProject;
import org.sablecc.objectmacro.launcher.ObjectMacro;

/**
 * Call ObjectMacro to generate Java fileName from ObjectMacro fileName.
 *
 * @author Hong Phuc Bui
 * @version 2.0-SNAPSHOT
 *
 * @phase generate-resources
 */
@Mojo(name = "objectmacro", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ObjectMacroCaller extends AbstractMojo {

	/**
	 * Output Type (Java, etc... depends on ObjectMacro)
	 */
	@Parameter(defaultValue = "java")
	private String language;
	@Parameter
	private String directory;
	@Parameter(defaultValue = "template")
	private String packagename;
	/**
	 * true => --generate-code, false => --no-code.
	 */
	@Parameter(defaultValue = "true")
	private boolean generateCode;
	/**
	 * true => strict false => lenient.
	 */
	@Parameter(defaultValue = "true")
	private boolean strict;
	/**
	 * informative => --informative quite => --quiet verbose => --verbose.
	 */
	@Parameter(defaultValue = "informative")
	private String informative;

	@Parameter(required = true)
	private String template;

	@Parameter(defaultValue = "${component.org.apache.maven.project.MavenProjectHelper}")
	private MavenProjectHelper projectHelper;

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	@Parameter(defaultValue = "${basedir}/src/main/objectmacro")
	private String objectmacroDirPath;
	
	private final String fileSep = System.getProperty("file.separator");
	//private String baseDir = System.getProperty("project.basedir");
	//private String baseDir ;
	private static final String GEN_CODE = "--generate-code";
	private static final String NO_CODE = "--no-code";
	private static final String STRICT = "--strict";
	private static final String LENIENT = "--lenient";
	private static final String INFORMATIVE = "--informative";
	private static final String VERBOSE = "--verbose";
	private static final String QUIET = "--quiet";
	private static final Set<String> packageNameNoGo = new HashSet<String>();

	static {// TODO add more 
		packageNameNoGo.add("..");
		packageNameNoGo.add("#");
		packageNameNoGo.add("@");
		packageNameNoGo.add("?");
		packageNameNoGo.add("/");
		packageNameNoGo.add("%");
		packageNameNoGo.add("$");
		packageNameNoGo.add(" ");
	}

	public void execute() throws MojoFailureException {
		try {
			if (projectHelper == null) {
				//getLog().warn("projectHelper is null");
				projectHelper = new DefaultMavenProjectHelper();
			}
			if (project == null) {
				getLog().warn("project is null");
			}

			directory = findOutputDirPath();
			Argument argv = parseArgument();
			if (argv != null) {
				if (needCompile(argv.getFile(), argv.getDirectory(), argv.getPackagename())) {
					getLog().info("Call ObjectMacro with argv:");
					getLog().info(argv.getArgv().toString());
					ObjectMacro.compile(argv.getStringArgv());
				} else {
					getLog().info("No need to compile template " + argv.getFile());
					getLog().info(" clean output directory to force re-compile template");
				}
				getLog().info("Add " + argv.getDirectory() + " to source and test");
				project.addCompileSourceRoot(argv.getDirectory());
				project.addTestCompileSourceRoot(argv.getDirectory());
			} else {
				//TODO: What is the convenient behavior if there are not 
				// templated files? I just put an warning out on screen.
				getLog().warn("no tag <template> found");
			}
		} catch (RuntimeException ex) {
			throw new MojoFailureException("Compile template file error: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new MojoFailureException("Compile template file error: " + ex.getMessage(), ex);
		}

	}

	private Argument parseArgument() {
		Argument a = new Argument();
		// TODO: optimize here, check the tag <file> first.
		String fileName = template;
		if (fileName == null) {
			getLog().warn("Configuration fail, cannot find the tag <template>");
			return null;
		} else {
			if (isFileNameValid(fileName)) {
				// option "-t language"
				if (isOptionValid(language)) {
					String localLanguage = language.trim();
					a.setLanguage(localLanguage);
				} else {
					throw new RuntimeException(language + " is not a valid option for output language");
				}

				// option "-d directory" // TODO: check validation directory
				if (isOptionValid(directory)) {
					String localDirectory = directory.trim();
					File outputDir = new File(localDirectory);
					if (!outputDir.isAbsolute()) {
						outputDir = new File(project.getBasedir(), localDirectory);
					}
					a.setDirectory(outputDir.getAbsolutePath());
				} else {
					throw new RuntimeException(directory + " is not a valid option for destinated directory");
				}

				// option "-p packagesname"
				if (isPackageNameValid(packagename)) {
					String localPackagename = packagename.trim();
					a.setPackagename(localPackagename);
				} else {
					throw new RuntimeException("package name not valid:" + packagename.trim());
				}

				// option "--generate-code" or "--no-code"
				String localGenerateCode = generateCode ? GEN_CODE : NO_CODE;
				a.setGenerateCode(localGenerateCode);

				// option "--strict" or "--lenient"
				String localStrict = strict ? STRICT : LENIENT;
				a.setStrict(localStrict);

				// option "--quiet" or "--informative" or "--verbose"
				String localInformative = INFORMATIVE;
				//String i = informative;
				informative = informative.trim().toLowerCase();
				if (informative.equals("quiet")) {
					localInformative = QUIET;
				} else if (informative.equals("informative")) {
					localInformative = INFORMATIVE;
				} else if (informative.equals("verbose")) {
					localInformative = VERBOSE;
				}
				a.setInformative(localInformative);
				File templateFile = guessTemplaceFile(template);
				
				a.setFile(templateFile.getAbsolutePath());
			} else {
				getLog().warn("Configuration fail, file name: " + fileName + " invalid");
				return null;
			}
		}
		return a;
	}

	private File guessTemplaceFile(String templaceParam) {
		if (templaceParam.contains(File.separator)) {
			File templateFile = new File(templaceParam);
			if (!templateFile.isAbsolute()) {
				templateFile = new File(project.getBasedir(), templaceParam);
			}
			return templateFile;
		}else{
			File objectMacroDir = new File(objectmacroDirPath);
			File templateFile = new File(objectMacroDir, templaceParam);
			return templateFile;
		}
	}

	private String findOutputDirPath() {
		if (directory == null) {
			directory = new File(project.getBasedir(), "target/generated-sources/objectmacro/").getAbsolutePath();
		} else {
			File outputDir = new File(directory);
			if (!outputDir.isAbsolute()) {
				outputDir = new File(project.getBasedir(), "target/generated-sources/" + directory);
				directory = outputDir.getAbsolutePath();
			}
		}
		return directory;
	}

	boolean isOptionValid(String option) {
		if (option == null) {
			return false;
		}
		if (option.trim().length() == 0) {
			return false;
		}
		return true;
	}

	boolean isPackageNameValid(String pn) {
		if (!isOptionValid(pn)) {
			return false;
		}
		if (pn.trim().startsWith(".")) {
			return false;
		}
		for (String n : packageNameNoGo) {
			if (pn.contains(n)) {
				return false;
			}
		}
		return true;
	}

	// TODO: make more restrict here
	private boolean isFileNameValid(String file) {
		assert file != null;
		return true;
	}

	private boolean needCompile(String template, String directory, String packagename) {
		File templageFile = new File(template);
		if (templageFile.isFile()) {
			String destinatePath
					= directory
					+ packagename.replace(".", "/");
			getLog().info("Check timestamp for the directory: " + destinatePath);
			File destinateDir = new File(destinatePath);
			if (destinateDir.isDirectory()) { // if the last part of package is already a director
				long lastModiTemplate = templageFile.lastModified();
				long lastModiOutputPackage = destinateDir.lastModified();
				if (lastModiTemplate > lastModiOutputPackage) {
					return true;
				} else {
					return false;
				}
			} else {// if the last part of the package is not a directory/not exist
				return true;
			}
		} else {
			return true;
		}
	}
}
