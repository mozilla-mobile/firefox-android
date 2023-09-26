#! /bin/bash

# some action that gets the product type, ie Firefox, Focus, Klar
echo "PRODUCT_TYPE=Firefox" >> ./test_dashboard.env

# some action that gets the release type, ie RC, Beta, etc
echo "RELEASE_TYPE=RC" >> ./test_dashboard.env