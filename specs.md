16KiB in size, or 4096 words
each block has 16-word block size

design is

address
[18 bits]  [8 bits]     [4 bits]        [2 bits]
Tag bits -- Index --- Block offset -- Byte offset
(31 - 14)  (13 - 6)    (5 - 2)         (1 - 0)















#### OPTIMISATIONS
remove multiplexor and replace with 2 rams, one smaller tag RAM and larger data RAM
