package de.htwds.pluginusertest;

import de.htwsaarland.sql.imp.template.MCreateTable;





/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
		MCreateTable tab = new MCreateTable("hallo");
		System.out.println(tab.toString());
		
		TemplateUser u = new TemplateUser();
		u.useTemplate("test");
    }
}
