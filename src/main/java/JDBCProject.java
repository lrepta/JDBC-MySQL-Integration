import java.io.FileReader;
import java.sql.*;
import java.util.*;

public class JDBCProject {

    // Method that runs at the very start, creating the employee and department tables
    // in the database. If an error is encountered here, the program will exit, because
    // none of the queries can be run properly if the tables are not set up
    public static void setupTables(Connection conn) {

        // Query to create the employee table
        // ename is the primary key
        // salary is numeric, so extra care must be taken when assign it's value
        String employeeTableCreationString = "CREATE TABLE employee (\n" +
                "ename varchar(25),\n" +
                "dept_name varchar(25),\n" +
                "salary numeric(10, 0),\n" +
                "city varchar(25),\n" +
                "PRIMARY KEY (ename)\n" +
                ");";
        // Query to create the department table
        // dept_name is the primary key
        // mname is a foreign key referencing some employee in employee table
        String departmentTableCreationString = "CREATE TABLE department (\n" +
                "dept_name varchar(25),\n" +
                "mname varchar(25),\n" +
                "PRIMARY KEY (dept_name),\n" +
                "FOREIGN KEY (mname) REFERENCES employee(ename)\n" +
                ");";

        // Try executing both queries, if either one fails, program should exit
        // Because if one table is not set up, the whole program will break
        try (
                PreparedStatement stmt1 = conn.prepareStatement(employeeTableCreationString);
                PreparedStatement stmt2 = conn.prepareStatement(departmentTableCreationString);
        ) {
            stmt1.executeUpdate();
            System.out.println("Successfully created the employee table");
            stmt2.executeUpdate();
            System.out.println("Successfully created the department table");
            stmt1.close();
            stmt2.close();
        }
        catch(SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Failed to create the tables, exiting");
            System.exit(-1);
        }
    }

    // Method for Transaction Code 1
    // Deletes the employee associated with the given name, if such an employee
    // does not exist, prints not found, and does not execute the statement
    public static void deleteEmployee(Connection conn, String deleteString) {
        String[] stringArgs = deleteString.split(" ");

        // Line should be: 1 (name of employee)
        if (stringArgs.length > 2) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "1 ename");
            return;
        }
        if (stringArgs.length < 2) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "1 ename");
            return;
        }
        String transcode = stringArgs[0];
        String ename = stringArgs[1];

        // Check if an employee of the given name even exists, before trying to remove them
        try(PreparedStatement nameExists = conn.prepareStatement("Select count(*) from employee where ename = ?;");
        ) {
            nameExists.setString(1, ename);

            ResultSet rs =  nameExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            if (resultInt == 0) {
                System.out.println("Not Found: " + ename);
                return;
            }
            // If the employee does exist, set to NULL the mname of all departments
            // in department which had the given employee as their manager
            try(PreparedStatement updateDept = conn.prepareStatement("update department " +
                    "set mname = NULL" +
                    " where mname = ?;");
            ) {
                updateDept.setString(1, ename);

                updateDept.executeUpdate();
            } catch (SQLException sqle) {
                System.out.println("Exception: " + sqle);
                System.out.println("Error when trying to null department managers before the associated " +
                        "employee was deleted");
            }
            try(PreparedStatement deleteEmp = conn.prepareStatement("delete from employee where ename = ?;");
            ) {
                deleteEmp.setString(1, ename);

                deleteEmp.executeUpdate();
                System.out.println("Deleted Employee: " + ename);
            } catch (SQLException sqle) {
                System.out.println("Exception: " + sqle);
                System.out.println("Error when attempting to delete the employee: " + ename);
            }
        } catch (SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Error when checking if an employee by the name of: " + ename + " exists");
        }
    }

    // Helper method for Transaction code 2
    // Helper method to check if the salary the user is trying to insert
    // is actually numeric. It should be in the format: 200000 for 200,000
    // This is used because salary is a numeric, not varchar, and as such
    // needs an actual numeric value
    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    // Method for Transaction Code 2
    // Inserts a new employee tuple with the given (name department salary city)
    // Checks if there is already an employee with the given name, and if so does not
    // execute the insertion, otherwise insert the new employee tuple with the name given
    public static void insertEmployee(Connection conn, String insertString) {
        String[] stringArgs = insertString.split(" ");

        // Line should be of the the form: 5 ename dept_name salary city
        if (stringArgs.length > 5) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "2 ename dept_name salary city");
            return;
        }
        if (stringArgs.length < 5) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "2 ename dept_name salary city");
            return;
        }
        String transcode = stringArgs[0];
        String ename = stringArgs[1];
        String dept_name = stringArgs[2];
        String salary = stringArgs[3];

        // Check that the salary is a valid number, otherwise don't execute the statement
        // Needed because the salary is declared as a numeric, rather than a varchar
        if (!isNumeric(salary)) {
            System.out.println("Salary must be a valid number");
            return;
        }

        String city = stringArgs[4];

        // Check if the given name already exists in the employee table,
        // and if so, do not execute the statement
        try(PreparedStatement nameExists = conn.prepareStatement("Select count(*) from employee where ename = ?");
        ) {
            nameExists.setString(1, ename);

            ResultSet rs =  nameExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            // If the name exists in the table already, return;
            if (resultInt == 1) {
                System.out.println("Duplicate Name: " + ename);
                return;
            }
            // Since the name is not in the table, insert the new tuple for the
            // given employee name, using a prepared statement for security
            try(PreparedStatement insertEmp = conn.prepareStatement("insert into employee values(?, ?, ?, ?)");
            ) {
                insertEmp.setString(1, ename);
                insertEmp.setString(2, dept_name);
                insertEmp.setString(3, salary);
                insertEmp.setString(4, city);

                insertEmp.executeUpdate();
                System.out.println("Added Employee: " + ename + " " + dept_name + " " + salary + " " + city);
            } catch (SQLException sqle) {
                System.out.println("Exception: " + sqle);
                System.out.println("Failed to insert new employee: " + ename + " " + dept_name
                        + " " + salary + " " + city);
            }
        } catch (SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Error when checking if the ename: " + ename + " already existed in the table");
        }
    }

    // Method for Transaction code 3
    // Deletes a department from the department table
    // If the department does not exist in the table, the statement is not executed
    // For all employees listed in employee with the given department as their dept_name,
    // set their dept_name to NULL
    public static void deleteDepartment(Connection conn, String deleteString) {
        String[] stringArgs = deleteString.split(" ");

        // Line should be of the form: 3 (name of the department)
        if (stringArgs.length > 2) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "3 dept_name");
            return;
        }
        if (stringArgs.length < 2) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "3 dept_name");
            return;
        }
        String transcode = stringArgs[0];
        String dept_name = stringArgs[1];

        // Check if a department of the given name exists
        try(PreparedStatement deptExists = conn.prepareStatement("Select count(*) from department where dept_name = ?");
        ) {
            deptExists.setString(1, dept_name);

            ResultSet rs = deptExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            if (resultInt == 0) {
                System.out.println("Not Found: " + dept_name);
                return;
            }
            // Delete the department of the given name from the department table
            try(PreparedStatement deleteDept = conn.prepareStatement("delete from department where dept_name = ?");
            ) {
                deleteDept.setString(1, dept_name);

                deleteDept.executeUpdate();
                System.out.println("Deleted: " + dept_name + " Department");
            } catch (SQLException sqle) {
                System.out.println("Exception: " + sqle);
                System.out.println("Error when attempting to delete department: " + dept_name);
            }
            // For every employee in the department that was just deleted, set their
            // dept_name to NULL in the employee table
            try(PreparedStatement updateEmp = conn.prepareStatement("update employee " +
                    "set dept_name = null" +
                    " where dept_name = ?");
            ) {
                updateEmp.setString(1, dept_name);
                updateEmp.executeUpdate();
            } catch (SQLException sqle) {
                System.out.println("Exception: " + sqle);
                System.out.println("Error when attempting to set to null the dept_name of every employee in the " +
                        " now deleted " + dept_name + " department");
            }
        } catch (SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Error when checking if department: " + dept_name + " exists");
        }
    }

    // Method for transaction code 4
    // Inserts a department into the database
    // It first checks that an employee with the given manager name exists,
    // then, if a department of the given department name exists, it will delete it
    // and then insert a new department managed by the manager name given
    // Performs rigorous error checking to ensure that the insertion works correctly
    public static void insertDepartment(Connection conn, String insertString) {
        String[] stringArgs = insertString.split(" ");

        // Input line must be of the form: transaction code department name manager name
        if (stringArgs.length > 3) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "4 dept_name mname");
            return;
        }
        if (stringArgs.length < 3) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "4 dept_name mname");
            return;
        }
        String transcode = stringArgs[0];
        String dept_name = stringArgs[1];
        String mname = stringArgs[2];

        // Check that there is an employee with the given manager name
        try(PreparedStatement nameExists = conn.prepareStatement("Select count(*) from employee where ename = ?");
        ) {
            nameExists.setString(1, mname);

            ResultSet rs = nameExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            // If there is no employee with the given name, return and don't execute the statement
            if (resultInt == 0) {
                System.out.println("Manager name does not exist: " + mname);
                return;
            }

            // Checks if the given department name already exists, and if it does,
            // it will delete it so that a new department may be inserted
            try(PreparedStatement deptExists = conn.prepareStatement("Select count(*) from department where dept_name = ?");
            ) {
                deptExists.setString(1, dept_name);

                ResultSet rsDept = deptExists.executeQuery();
                rsDept.next();
                int resultIntDept = rsDept.getInt(1);
                // If the department already exists, delete it
                if (resultIntDept == 1) {
                    System.out.println("Department already exists, deleting: " + dept_name);
                    // Delete the department
                    try(PreparedStatement deleteDept = conn.prepareStatement("delete from department where dept_name = ?;");
                    ) {
                        deleteDept.setString(1, dept_name);

                        deleteDept.executeUpdate();
                    } catch (SQLException sqle) { System.out.println("Error deleting the department, Exception: " + sqle); }
                }
            } catch (SQLException sqle) { System.out.println("Error checking if the department Exists," + dept_name +
                    "\nException: " + sqle); }

            // Insert the department under the given manager into department
            try(PreparedStatement insertDept = conn.prepareStatement("insert into department values(?, ?)");
            ) {
                insertDept.setString(1, dept_name);
                insertDept.setString(2, mname);

                insertDept.executeUpdate();
                System.out.println("Added department: " + dept_name + " " + mname);
            } catch (SQLException sqle) { System.out.println("Error inserting the department, Exception: " + sqle); }
        } catch (SQLException sqle) { System.out.println("Error checking if the name exists, Exception: " + sqle); }
    }

    // Helper method for Transaction Code 5
    // Returns a HashSet containing the names of all employees under the given name,
    // or Null if they don't manage any
    // Used on each employee directly under the manager, to get the set of employees
    // under them, which are consequently indirectly under the manager
    public static HashSet<String> getEmployeesUnder(Connection conn, String managerName) {
        // The set of employees working directly under mname
        HashSet<String> employees = new HashSet<>();

        // Check if the given managerName manages any departments,
        // otherwise, just return null for the set of employees under them
        try(PreparedStatement managerExists =
                    conn.prepareStatement("Select count(*) from department where mname = ?");
        ) {
            managerExists.setString(1, managerName);

            ResultSet rs = managerExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            // If the employee does not manage any departments, just return null
            if (resultInt == 0) {
                return null;
            }

            // Get all the departments managed by the manager
            try(PreparedStatement getDeptsUnderManager =
                        conn.prepareStatement("select dept_name from department where mname = ?;");
            ) {
                getDeptsUnderManager.setString(1, managerName);

                // All the departments under this manager
                ResultSet deptsUnderManager = getDeptsUnderManager.executeQuery();

                while(deptsUnderManager.next()) {
                    // One of the departments managed by the manager
                    String currDept = deptsUnderManager.getString(1);

                    // Get all the employees in currDept -> directly under the manager
                    try(PreparedStatement getEmps =
                                conn.prepareStatement("select ename from employee where dept_name = ?;");
                    ) {
                        getEmps.setString(1, currDept);

                        ResultSet directEmployees = getEmps.executeQuery();
                        while(directEmployees.next()) {
                            // Add the currEmployee to the set (Directly under Mname)
                            String currEmployee = directEmployees.getString(1);
                            employees.add(currEmployee);
                        }
                    } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }
                }
            } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }
        } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }

        // Return the set containing all the employees under the manager
        return employees;
    }

    // Method for Transaction Code 5
    // Prints "All employees directly and indirectly under (name of the manager):"
    // and then the tab indented list of all employees with each on a new line
    // Handles keeping track of employees and already accounted for employees via
    // a HashSet and Queue.
    public static void listEmployeesUnder(Connection conn, String insertString) {
        String[] stringArgs = insertString.split(" ");
        // The set of employees working directly and indirectly under mname
        HashSet<String> employees = new HashSet<>();

        // Input line should only have the transaction code and manager name
        if (stringArgs.length > 2) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "5 mname");
            return;
        }
        if (stringArgs.length < 2) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "5 mname");
            return;
        }
        String transcode = stringArgs[0];
        String mname = stringArgs[1];

        // Check if the manager manages at least one department in department,
        // otherwise do not execute the statement
        try(PreparedStatement managerExists =
                    conn.prepareStatement("Select count(*) from department where mname = ?");
        ) {
            managerExists.setString(1, mname);

            ResultSet rs = managerExists.executeQuery();
            rs.next();
            // Check that there is at least one department under the manager
            int resultInt = rs.getInt(1);
            // If there are no departments managed by the manager, return
            if (resultInt == 0) {
                System.out.println("No department managed by the given name: " + mname);
                return;
            }

            // Get all the departments managed by mname
            try(PreparedStatement getDeptsUnderManager =
                        conn.prepareStatement("select dept_name from department where mname = ?;");
            ) {
                getDeptsUnderManager.setString(1, mname);

                // The departments managed by mname
                ResultSet deptsUnderManager = getDeptsUnderManager.executeQuery();

                while(deptsUnderManager.next()) {
                    // One of the departments managed by mname
                    String currDept = deptsUnderManager.getString(1);
                    // Get all the employees in currDept -> directly under mname
                    try(PreparedStatement getEmps =
                                conn.prepareStatement("select ename from employee where dept_name = ?;");
                    ) {
                        getEmps.setString(1, currDept);

                        ResultSet directEmployees = getEmps.executeQuery();
                        Queue<String> employeesToCheck = new LinkedList<String>();
                        while(directEmployees.next()) {
                            // Add the currEmployee to the set (Directly under Mname)
                            String currEmployee = directEmployees.getString(1);
                            employees.add(currEmployee);
                            employeesToCheck.add(currEmployee);
                        }
                        // Using a linked list implementation of a queue, repeatedly add
                        // indirect subordinates, until the queue is empty, signifying that
                        // every subordinate under the given manager has been checked
                        // for their own subordinates
                        while(!employeesToCheck.isEmpty()) {
                            // The current employee being checked for subordinates
                            String currEmp = employeesToCheck.remove();
                            // The subordinates of the current employee
                            HashSet<String> indirectEmpSet = getEmployeesUnder(conn, currEmp);

                            // If the current employee is not a manage of any department, move on
                            if (indirectEmpSet == null) {
                                continue;
                            }
                            // Get all the employees directly under currEmp
                            // and add them to the employee set -> indirectly under Mname
                            for(String emp : indirectEmpSet) {
                                // Only if this employee has not already been accounted for
                                // should they be added into the queue and set of subordinates
                                // This prevents it from being an infinite loop
                                if (!employees.contains(emp)) {
                                    employeesToCheck.add(emp);
                                    employees.add(emp);
                                }
                            }
                        }

                    } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }
                }
            } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }
        } catch (SQLException sqle) { System.out.println("Exception: " + sqle); }

        // Remove mname, as a manager is not under themself
        employees.remove(mname);

        // If all the departments managed by mname contain only mname as their sole employee
        if (employees.isEmpty()) {
            System.out.println("No employees directly or indirectly under " + mname);
        }

        // Print the tab indented name of every employee directly and indirectly under mname
        System.out.println("All employees directly and indirectly under " + mname + ":");
        for (String emp : employees) {
            System.out.println("\t" + emp);
        }
    }

    // Method for Transaction code 6
    // Prints "All departments managed by (name of the manager):"
    // and then the tab indented list of all departments with each on a new line
    // If there are no departments managed by the given employee, or if the employee
    // does not exist, it will tell you the error, and not execute the statement
    public static void listDepartments(Connection conn, String managerName) {
        String[] stringArgs = managerName.split(" ");

        // The input line should only have the transcode, and manager name
        if (stringArgs.length > 2) {
            System.out.println("Line has too many arguments, line should be of the form:\n\t" +
                    "5 mname");
            return;
        }
        if (stringArgs.length < 2) {
            System.out.println("Line has too few arguments, line should be of the form:\n\t" +
                    "5 mname");
            return;
        }
        String transcode = stringArgs[0];
        String mname = stringArgs[1];

        // Check that the manager name actually exists as the head of some department
        // This also handles the case where there is no such employee by that name in employee
        try(PreparedStatement managerExists =
                    conn.prepareStatement("Select count(*) from department where mname = ?");
        ) {
            managerExists.setString(1, mname);

            // Get the number of instances of the manager name in department
            ResultSet rs = managerExists.executeQuery();
            rs.next();
            int resultInt = rs.getInt(1);
            // If there is no such manager in department, return and dont execute the statement
            if (resultInt == 0) {
                System.out.println("No department managed by the given name: " + mname);
                return;
            }
            System.out.println("All of the departments managed by " + mname + ":");

            // Get all the departments managed by mname
            try(PreparedStatement getDeptsUnderManager =
                        conn.prepareStatement("select dept_name from department where mname = ?;");
            ) {
                getDeptsUnderManager.setString(1, mname);

                ResultSet deptsUnderManager = getDeptsUnderManager.executeQuery();

                // For each department, get it from the result set and print it tab indented
                // for ease of viewing
                while(deptsUnderManager.next()) {
                    // One of the departments managed by mname
                    String currDept = deptsUnderManager.getString(1);
                    System.out.println("\t" + currDept);
                }
            } catch (SQLException sqle) { System.out.println("Error when getting departments, Exception: " + sqle); }
        } catch (SQLException sqle) { System.out.println("Error checking departments for a mname, Exception: " + sqle); }
    }

    // Method to drop the department and employee tables of the given database
    // Used at the end of the program, or when an exception occurs in processing
    // the transfile, after the tables have been created
    public static void dropAllTables(Connection conn) {
        String dropEmployeeString = "drop table department;";
        String dropDeptString = "drop table employee;";
        try (PreparedStatement dropStmt1 = conn.prepareStatement(dropEmployeeString);
             PreparedStatement dropStmt2 = conn.prepareStatement(dropDeptString);) {
            System.out.println("Dropping Employee and Department tables");
            dropStmt1.executeUpdate();
            dropStmt2.executeUpdate();

            dropStmt1.close();
            dropStmt2.close();
        } catch (SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Failed to drop the tables, database may need manual cleaning");
        }
    }

    public static void main(String[] args) {
        // Reads in the database, userid, and password from the command line
        // For me, database was "companyDB", userid was "root", and password was my password

        Scanner dbInfo = new Scanner(System.in);
        System.out.println("Enter the name of the database:");
        String dbid = dbInfo.nextLine();
        System.out.println("Enter your user id:");
        String userid = dbInfo.nextLine();
        System.out.println("Enter your password:");
        String passwd = dbInfo.nextLine();

        String connectionString = "jdbc:mysql://localhost:3306/" + dbid + "?user=userid&password=passwd";
        // "jdbc:mysql://localhost:3306/NameOfTheDatabase?user=userid&password=passwd"
        try (Connection conn = DriverManager.getConnection(connectionString, userid, passwd);) {
            System.out.println("Successfully opened the database");

            // Create the tables before processing any lines from the transfile
            setupTables(conn);

            // Make sure that the transfile is in the same directory as src:
            // My folder/file setup for my maven IntelliJ project
            // JDBC MySQL Integration:
            //  .idea
            //  src
            //  target
            //  transfile.txt
            try (Scanner in = new Scanner(new FileReader("transfile.txt"));) {

                // Read each line of transfile.txt
                while(in.hasNextLine()) {
                    String currLine = in.nextLine();

                    // If a line is blank (only a newline character), just skip it
                    if (currLine.length() < 1) {
                        System.out.println("Line was blank, skipping");
                        continue;
                    }

                    // Check that the first character is a valid transaction code
                    char transCodeChar = currLine.charAt(0);
                    if (Character.isDigit(transCodeChar) == false) {
                        System.out.println("Invalid Transaction Code, first char of line was not a number");
                        continue;
                    }
                    int transcode = Character.getNumericValue(transCodeChar);
                    if (transcode < 1 || transcode > 6) {
                        System.out.println("Invalid Transaction Code, code must be a number between 1-6");
                        continue;
                    }

                    // Execute each statement based on the number given
                    // Error checking for correct form is then done in each method
                    switch (transcode) {
                        case 1:
                            deleteEmployee(conn, currLine);
                            break;
                        case 2:
                            insertEmployee(conn, currLine);
                            break;
                        case 3:
                            deleteDepartment(conn, currLine);
                            break;
                        case 4:
                            insertDepartment(conn, currLine);
                            break;
                        case 5:
                            listEmployeesUnder(conn, currLine);
                            break;
                        case 6:
                            listDepartments(conn, currLine);
                            break;
                    }
                }

            // Drop the tables after processing the transfile
            dropAllTables(conn);
            // After processing every line of the transfile, close the connection
            conn.close();

            System.out.println("\nProgram ran successfully, now exiting");
            System.exit(0);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex);
                System.out.println("Could not open the file, make sure it is in the root directory");
                dropAllTables(conn);
                System.exit(-1);
            }
        }
        catch(SQLException sqle) {
            System.out.println("Exception: " + sqle);
            System.out.println("Make sure you are entering the name of the database, " +
                    "your userid, and password correctly");
            System.exit(-1);
        }
    }
}