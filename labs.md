https://textbooks.elsevier.com/web/manuals/aspx?isbn=
https://textbooks.elsevier.com/web/manuals/aspx?isbn=9780128203316


https://www.elsevier.com/books-and-journals/book-companion/9780128203316

exploit temporal / spacial locality

Sets with multiple blocks
    if a set has multiple blocks it will contain adjacent adresses. e.g. if a set is block size 4 it will contain addresses [0x100], [0x101],[0x102], [0x103]
    with a block offset
    as in from the requested memory address, the required block is known
    Memory Address [Tag (x bits)] [Set]  [Block Offset][Byte Offset]
    ------------------ 27 bits -- 1 bit ---- 2 bits ------ 2 bits --

^^^^^^ explanation might be wrong

caches can have multiple sets and blocks
and each block can contain N amount of words, being block size

a block is a single cache line

block size
a block contains a valid bit, tag, and N words storages



Q1: Where Can a Block be placed in Cache

direct mapped: (Block Address) Mod (No. of blocks in cache)
block address = byte address / bytes per block


16 KiB cache design
lab 1 :
create cache with idle state

lab 2: create compare stage
2a: create cache read
2b: create cache-write

lab 3: write-back stage

lab 4: allocate stage


Optimisations:
lab 5:
compare does both compare and read/write in one cycle/state
split compare Tag state into two states; compare and cache access (read/write)

lab 6: 
add write buffer to save dirty block

