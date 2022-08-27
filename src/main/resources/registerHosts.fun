def _cmdCharset='GBK';

def res=cmd# ipconfig;

def subStartIndex=res.indexOf('无线局域网适配器 WLAN');
res=res.substring(subStartIndex);
def subEndIndex=res.indexOf('子网掩码');
res=res.substring(0,subEndIndex);

for(line:res.split(esc('\n'))){
    if(line.strip().startsWith('IPv4')){
        print(line.split(':')[1].strip());
        break;
    }
}