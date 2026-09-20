sed -i '/cat rapc_output.txt/c\        ant build 2> rapc_errors.txt > rapc_output.txt || true\n        cat rapc_output.txt\n        grep -i "requires signing" rapc_output.txt > security-api-report.txt || true\n        grep -i "Warning!:" rapc_output.txt >> security-api-report.txt || true\n        echo "RAPC Security Report Generated:"\n        cat security-api-report.txt' .github/workflows/build-bbsb.yml

sed -i '/blackberry\/\*.jar/a \          blackberry/security-api-report.txt\n          blackberry/rapc_output.txt' .github/workflows/build-bbsb.yml
