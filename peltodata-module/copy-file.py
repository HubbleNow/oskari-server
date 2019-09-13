import sys
import json
from shutil import copyfile


print("This is the name of the script: ", sys.argv[0])
print("Number of arguments: ", len(sys.argv))
print("The arguments are: " , str(sys.argv))

json_file = sys.argv[1]

print("json file is " + json_file)

with open(json_file, 'r') as f:
        contents = json.load(f)

print("contents ", contents);

input_file = contents["input_file"]
output_file = contents["output_file"];

copyfile(input_file, output_file)