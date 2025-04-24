@echo off
echo Testing API Client...
echo.

REM Test GET requests
echo Testing GET endpoints...
java ApiTester get https://jsonplaceholder.typicode.com/posts/1
timeout /t 2 > nul

echo Testing GET with query params...
java ApiTester get https://jsonplaceholder.typicode.com/posts?userId=1
timeout /t 2 > nul

REM Test POST request
echo Testing POST with JSON...
java ApiTester post https://jsonplaceholder.typicode.com/posts "{\"title\":\"Hello\",\"body\":\"Test post\",\"userId\":1}"
timeout /t 2 > nul

REM Test with headers
echo Testing with custom headers...
java ApiTester get https://jsonplaceholder.typicode.com/posts/1 "Accept: application/json" "X-Custom-Header: test"
timeout /t 2 > nul

REM Test error cases
echo Testing error response...
java ApiTester get https://jsonplaceholder.typicode.com/posts/999
timeout /t 2 > nul

echo.
echo Tests completed! 