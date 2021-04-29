# Level Templates

These reflect the parameters (memory size and preset values) of the actual game levels.

- Levels before 14 aren't included because you can't use the jump-if-negative instruction in them, so writing code by hand should be simple enough that a compiler doesn't add value.

- In levels before 19, the `bump+` and `bump-` instructions aren't available. Don't use the `++` and `--` operators in these levels. (`hrmc` won't stop you, but Human Resource Machine won't let you paste in the resulting program.)

- In levels before 29, you can't use dereferencing in `copyfrom` and `copyto`. Don't use the `*` operator in these levels. (But again, `hrmc` won't stop you.)
