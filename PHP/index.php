<?php

header ("text/html");
require_once ("db_login.php");
mysql_select_db('dulimarh');
$query = 'SELECT * FROM checkout, student '.
    'WHERE checkout.userid = student.userid';
$result = mysql_query ($query);
if (mysql_num_rows($result) == 0) {
    print "No phone is currently checked out";
}
else {
    print '<table cellspacing="0" border="1" cellpadding="4" align="center">';
    print '<tr><th>Phone ID</th><th>UserId</th><th>Name</th>' .
        '<th>Checkout Date</th><th>Signature</th></tr>';
    while ($row = mysql_fetch_assoc($result)) {
        echo '<tr>';
        cell ($row['device_id']);
        cell ($row['userid']);
        cell ($row['name']);
        cell (strftime("%Y-%b-%d %T", $row['checkout']));
        cell ('<img src="' . $row['signature'] . '" height="75" />');
        echo '</tr>';
    }
    print '</table>';
}

function cell ($item)
{
    echo '<td>' . $item . '</td>';
}
?>
