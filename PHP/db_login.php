<?php
   $username = 'put_your_eos_username_here';
   $password = 'put_your_mysql_db_password_here';
   $hostspec = 'localhost';
   $db = mysql_connect ($hostspec, $username, $password);
   if (mysql_errno()) {
       print mysql_errno() . ": " . mysql_error() . "\n";
       exit;
   }
?>
