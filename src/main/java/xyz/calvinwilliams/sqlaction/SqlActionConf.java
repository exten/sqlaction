/*
 * sqlaction - SQL action object auto-gencode tool based JDBC for Java
 * author	: calvin
 * email	: calvinwilliams@163.com
 *
 * See the file LICENSE in base directory.
 */

package xyz.calvinwilliams.sqlaction;

import java.util.*;

public class SqlActionConf {
	public String							database ;
	public LinkedList<SqlActionConfTable>	tables ;
	public String							javaPackage ;
}
