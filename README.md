# Alpaca

A tiny scripting language for web browser automation.

## Example

```ruby
#!/usr/bin/env alpaca

visit("https://github.com")
click(link("Sign in"))
fill("#login_field", "user")
fill("#password", "password")
submit()
```

## Syntax

## Variable

```js
var user = "tototoshi"
```

## Function

### Basic functions
```ruby
visit("http://google.com")
click("#selector")
fill("#selector", "text...")
submit()
```

### User functions
```ruby
def login(user, password) {
  ...
}

login(user, password)
```

## Load other files

```ruby
require "util.alpaca"
```

## Comment

```ruby
# This line is ignored
```

## Read environment variable

```ruby
print $HOME #=> /Users/toshi
```

## Execute shell command

```bash
print $(date +%Y%m%d) #=> 20131120
```


