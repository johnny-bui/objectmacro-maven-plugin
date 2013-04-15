package de.htwds.objectmacroplugin;

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
 * Call ObjectMacro to generate Java file from ObjectMacro file.
 *
 * @author Hong Phuc Bui
 * @version 2.0-SNAPSHOT
 *
 * @phase generate-resources
 */
@Mojo(name = "objectmacro", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
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
	 * informative => --informativ quite => --quiet verbose => --verbose.
	 */
	@Parameter(defaultValue = "informative")
	private String informative;
	
	@Parameter(required=true)
	private String template;

	@Parameter(defaultValue="${component.org.apache.maven.project.MavenProjectHelper}")
	private MavenProjectHelper projectHelper;
	
	@Parameter(defaultValue="${project}")
	private MavenProject project;
			
	private final String fileSep = System.getProperty("file.separator");
	private String baseDir = System.getProperty("project.basedir");
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
		packageNameNoGo.add(" ");
	}
	
	public void execute() throws MojoFailureException {
		try {
			if (projectHelper==null){
				//getLog().warn("projectHelper is null");
				projectHelper = new DefaultMavenProjectHelper();
			}
			if (project == null){
				getLog().warn("project is null");
			}
			constructOutDir();
			
			Argument argv = parseArgument(/*m*/);
			if (argv != null) {
				getLog().info("call ObjectMacro with argv:");
				getLog().info(argv.getArgv().toString());
				ObjectMacro.compile(argv.getStringArgv());

				getLog().info("add " + argv.getDirectory() + " to resources and test");
				project.addCompileSourceRoot(argv.getDirectory());
				project.addTestCompileSourceRoot(argv.getDirectory());
			}else{
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
		String file = template;  //m.get("file");
		if (file == null) {
			getLog().warn("Configuration fail, cannot find the tag <file>");
			return null;
		} else {
			if (isFileNameValid(file)) {
				// option "-t language"
				if(isOptionValid(language)){
					String localLanguage = language.trim();
					a.setLanguage(localLanguage);
				}else{
					throw new RuntimeException(language + " is not a valid option for output language");
				}
				
				// option "-d directory" // TODO: check validation directory
				if (isOptionValid(directory)){
					String localDirectory = directory.trim();
					a.setDirectory(localDirectory);
				}else{
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
				a.setFile(file);
			} else {
				getLog().warn("Configuration fail, file name: " + file + " invalid");
				return null;
			}
		}
		return a;
	}

	private String constructOutDir() {
		if (baseDir == null) {
			baseDir = ".";
		}
		if (directory == null) {
			directory = baseDir + fileSep
					+ "target" + fileSep
					+ "generated-sources" + fileSep
					+ "objectmacro" + fileSep;
		} else {
			if (!directory.endsWith(fileSep)) {
				directory += fileSep;
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
}