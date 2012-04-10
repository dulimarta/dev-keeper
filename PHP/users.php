<?php
header ("text/plain");
require_once ("db_login.php");
    mysql_select_db('dulimarh');
    $query = "SELECT * FROM student";
    $result = mysql_query ($query);
    print '{ "users" : [';
    $count = 0;
    while ($row = mysql_fetch_object($result)) {
        if ($count > 0) print ",";
        printf ('{"userid":"%s", "name":"%s"}', $row->userid, $row->name);
        $count++;
    }
    print ']}';

?>
