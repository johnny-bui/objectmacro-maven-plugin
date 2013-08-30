package de.htwds.objectmacroplugin;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author phucluoi
 * @version Feb 3, 2013
 */
public final class Argument {
	private List<String> argv;
	private String l;
	private String d;
	private String p;
	private String g;
	private String s;
	private String i;
	private String f;
	
	public Argument(){
		argv = new ArrayList<String>();
	}
	

	public void setLanguage(String language){
		l = language;
		argv.add("-t");
		argv.add(l);
	}

	public void setDirectory(String directory){
		d = directory;
		argv.add("-d");
		argv.add(d);
	}
	/** 
	 * need to check timestamp
	 */
	public String getDirectory(){
		return d;
	}
	
	public void setPackagename(String packagename){
		p = packagename;
		argv.add("-p");
		argv.add(p);
	}
	
	/**
	 * need to check timestamp
	 */
	public String getPackagename(){
		return p;
	}
	
	public void setGenerateCode(String generateCode){
		g = generateCode;
		argv.add(g);
	}

	public void setStrict(String strict){
		s = strict;
		argv.add(s);
	}

	public void setInformative(String informative){
		i = informative;
		argv.add(i);
	}

	public void setFile(String file){
		f = file;
		argv.add(f);
	}
	
	public String getFile(){
		return f;
	}
	
	public String[] getStringArgv(){
		return argv.toArray(new String[0]);
	}

	public List<String> getArgv(){
		List<String> clone = new ArrayList<String>(argv.size());
		for(String c : argv){
			clone.add(c);
		}
		return clone;
	}
	
}
