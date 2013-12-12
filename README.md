# Alpaca

A tiny scripting language for web browser automation.

## Example

```ruby
#!/usr/bin/env alpaca

visit("https://github.com")
click(t"Sign in")
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
#### visit

```ruby
visit("http://google.com")
```

#### click

```ruby
# click by css selector
click("#id")

# click by xpath 
click(x"//....")

# click by name
click(n"username")

# click by link text
click(t"link text")

# click by partial link text
click(pt"partial link text")
```

#### fill

```ruby
fill("selector", "text...")
```

#### select

```ruby
select("selector", value)
```

#### submit

```ruby
submit()
```

### User functions
```ruby
def login(user, password) {
  ...
}

login(user, password)
```

## if-else

```c
if (len(args) == 1) {
  print 1
} else if (len(args) == 2) {
  print 2
} else {
  print 0
}
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


