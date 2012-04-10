<?php

header ("text/plain");
require_once ("db_login.php");
mysql_select_db('dulimarh');
if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $device = $_REQUEST['device'];
    if (isset($device)) {
        $query = 'SELECT * FROM checkout, student ' .
            'WHERE device_id = "' . $device .  '" '.
            'AND checkout.userid = student.userid';
        $result = mysql_query ($query);
        if (mysql_num_rows($result) == 0) {
            print '{"checkout": 0}';
        }
        else {
            $arr = mysql_fetch_array($result);
            printf ('{"checkout":%d, "co_time":"%s", "user":"%s", "name":"%s"}',
                $arr['checkout'], strftime("%T %Y-%b-%d", $arr['checkout']), 
                $arr['userid'], $arr['name']);
        }
    }
    else {
        $query = 'SELECT * FROM checkout, student ' .
            'WHERE checkout.userid = student.userid';
        $result = mysql_query ($query);
        print '{"checkouts": [';
        $count = 0;
        while ($row = mysql_fetch_assoc($result)) {
            if ($count > 0) echo ",";
            echo json_encode($row);
            $count++;
        }
        print ']}';
    }
}
else {
    /*
    foreach ($_POST as $k => $v) {
        printf ("Param %s\n", $k);
    }
    foreach ($_FILES as $k => $v) {
        printf ("File %s\n", $k);
    }
    */
    $user = $_POST['name'];

    /* must convert from msec to sec */
    $checkout_time = (int) ($_POST['time'] / 1000);
    $device = $_POST['device'];
    $log = fopen ("debug.log", "a");
    $imgout = "images/" .$user . ".png";
    fprintf ($log, "Device %s is checked out to %s on %s\n", 
            $device, $user, strftime("%T %Y-%b-%d", $checkout_time));
    $img = fopen ($imgfile, "wb");
    $image = $_FILES['image'];
    $imgin = $image['tmp_name'];
    rename ($imgin, $imgout);
    print_r ($image);
    fwrite ($img,  $_FILES['image']);
    fclose ($img);
    chmod ($imgout, 0646);
    chown ($imgout, "dulimarh");

    $query = 'SELECT * FROM checkout WHERE device_id = "' . $device . '"' ;
    $result = mysql_query ($query);
    if (mysql_num_rows ($result) == 0) {
        $query = 'INSERT checkout(device_id, userid, checkout, signature) '
            . ' VALUES("' . $device . '","' . $user . '",' . 
            $checkout_time . ',"'.  $imgout . '")';
        $result = mysql_query ($query);
        if ($result) {
            echo "Successful insertion\n";
        }
        else {
            echo "Error running " . $query . "\n";
        }
    }
    else {
        printf ("%s was checked out by %s\n", $device, $user);
    }
    fclose ($log);
}
    ?>
