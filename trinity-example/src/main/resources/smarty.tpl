<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Greeting</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
    <p>{$message}</p>
<ul>
{for $foo=1 to 3}
    <li>{$foo}</li>
{/for}
</ul>
</body>
</html>