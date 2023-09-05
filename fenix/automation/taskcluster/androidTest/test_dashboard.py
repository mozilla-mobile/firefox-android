import os

print("Printing ENV VARS")

for key, value in os.environ.items():
    print(f"{key} = {value}")

print("All Done!")