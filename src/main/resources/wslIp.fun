

func('getWinIp','originIp','originMask',@block{
    def addr=new('InetSocketAddress',originIp, 0);
    def maskAddr=new('InetSocketAddress',originMask, 0);
    def ip = addr.getAddress().getAddress();
    def mask = maskAddr.getAddress().getAddress();
    def joiner = new('StringJoiner','.');
    for(i:4){
        def res = T(Byte).toUnsignedInt(ip[i]) & T(Byte).toUnsignedInt(mask[i]);
        print((i == 3 ? res + 1 : res));
        joiner.add((i == 3 ? res + 1 : res) + '');
    };
});

getWinIp('172.27.215.84','255.255.240.0');