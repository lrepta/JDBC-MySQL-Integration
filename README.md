I used MySQL + a maven project via IntelliJ

Description:

    This project creates a connection between a user created database in a 
    MySQL server and a java program via JDBC, creating two new tables: employee, 
    and department in the specified database.

    The schema which you are using for the tables should already exist before 
    attempting to run the program (In MySQL I have a schema called companyDB, 
    which the program connects to).

    The program reads in a transfile.txt and parses queries based on the 
    transaction code given at the start of the line,and the subsequent 
    arguments. Rigorous error checking and exception handling is done throughout 
    the program

    Uses prepared statements to prevent SQL injection attacks, and only permits 
    executing a set of allowed queries as designated by their Transaction Code. 
    The user may customize the queries by changing the parameters in the 
    transfile.txt, which is rigorously error checked before allowing execution.

How to use:

    Make sure the transfile.txt is in the root folder of the project, or else 
    it will not be found

    When the program starts, it will ask you for the name of the database (schema)
    that you want to connect to (for me this was my companyDB schema in MySQL), 
    your userid to log in to MySQL (for me I just did "root"), and your MySQL 
    password. If all these are entered correctly, then it will connect to the 
    database and begin processing the transfile.txt

    The program reads every line of the transfile.txt and attempts to interpret them
     as some transaction code and arguments. Rigorous error checking is used to make
      sure that each line is evaluated or skipped correctly.

    Transaction Code 1.) DELETE from employee where eName = userEnteredEmployeeName
        Deletes the employee associated with the given name from the designated 
        department, if such an employee does not exist, prints not found, and does
        not execute the statement.

    Transaction Code 2.) INSERT INTO employee values(name, department, salary, city)
        Inserts a new employee tuple with the given (name department salary city) into
        the employee table. Checks if there is already an employee with the given name,
        and if so does not execute the insertion, otherwise insert the new employee 
        tuple with the name given.

    Transaction Code 3.) DELETE from department where dept_name = userEnteredDept_name 
        Deletes a department from the department table. If the department does not 
        exist in the table, the statement is not executed. For all employees listed
        in employee with the given department as their dept_name, set their dept_name
        to NULL.

    Transaction Code 4.) insert into department values(dept_name, managerName)
        Inserts a department into the department table. It first checks that an 
        employee with the given manager name exists, then, if a department of the 
        given department name exists, it will delete it and then insert a new 
        department managed by the manager name given. Performs rigorous error checking
        to ensure that the insertion works correctly

    Transaction Code 5.): 
            Query #1.) select dept_name from department where mname = userEnteredManagerName
            Query #2.) select ename from employee where dept_name = currDept_name
            for each department returned by #1: 
                execute query #2
                add all employees returned to the subordinate queue
            while subordinate queue is not empty:
                currEmployee = queue.pop()
                add currEmployee to subordinate set
                if currEmployee has not been processed already:
                    execute query #1 for the currEmployee
                    for each department returned by #1:
                        execute query #2, and add any new employees to the queue
        Prints "All employees directly and indirectly under (name of the manager):", and 
        then the tab indented list of all employees with each on a new line. Handles 
        keeping track of employees and already accounted for employees via a HashSet and 
        Queue. An employee is indirectly under a given manager if said manager manages a 
        department to which the direct manager of the employee is a member of, and if the 
        employee is a manager of any department, it recursively adds all their subordinates, 
        and so on until all employees indirectly under the given manager are added.

    Transaction code 6
        Prints "All departments managed by (name of the manager):", and then the tab 
        indented list of all departments with each on a new line. If there are no 
        departments managed by the given employee, or if the employee does not exist, 
        it will tell you the error, and not execute the statement.
    
    The salary attribute is interpreted to be a numeric, rather than a string, so error 
    checking is done to ensure that this attribute is actually a valid number

    The output of all queries, errors, exceptions, lines skipped, etc. is printed to the 
    console. Information about the query executed will be printed to the console. For 
    example, adding an Employee named Mary to the Software department in Chicago with a 
    Salary of 200,000 will print the entire tuple added:
        "Added employee: Mary Software 200000 Chicago"