# simple image server

## Usage

Run the project directly:

    $ clojure -m images.core

Build an uberjar:

    $ clojure -A:uberjar

Run that uberjar:

    $ java -jar images.jar

## Docker

Build uberjar:

    $ clojure -A:uberjar
    
Build Docker image:

    $ docker build -t images:latest .

Run Docker container:

    $ docker run -d --name images -p 9090:9090 images:latest
    
## Examples

Upload Image:
    
    $ http --verbose -f POST http://localhost:9090/upload image@image.jpg
    
Response 

```
{
    "images": [
        {
            "small": "http://localhost:9090/images/9c76bcba-3cef-4001-93e2-34b5bbc9700d-small.jpg"
        },
        ...]
}
```

Download Image Using wget:
    
    $ wget http://localhost:9090/images/9c76bcba-3cef-4001-93e2-34b5bbc9700d-small.jpg

## License

Copyright © 2020 Rodionovaanastasia

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
