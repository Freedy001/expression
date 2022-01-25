//检验正则表达式
for(i:condition(@block{true})){
    printInline('请输入正则表达式(exit终止程序):');
    def reg=stdin();
    if(reg.equals('exit')){
        break;
    };
    def pattern=T(java.util.regex.Pattern).compile(reg);
    for(ii:condition(@block{true})){
        printInline('输入匹配字符串(0退出):');
        def str=stdin();
        if(str.equals('0')){
            break;
        };
        def match=pattern.matcher(str);
        print('is match? -->'+match.matches());
        match=pattern.matcher(str);
        for(m:condition(@block{match.find()})){
            print('-----------------------------第' + m + '次匹配-------------------------------------');
            for(g:match.groupCount()+1){
                print('第'+ g +'组匹配:'+match.group(g));
            };
        };
        print('****************************************over*********************************************');
    };
}
