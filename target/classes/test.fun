def arr=[];
def testArr=[];

for(i:1000){
    def num=int(T(java.lang.Math).random()*1000);
    #arr.add(#num);
    #testArr.add(#num);
};



quickSort(#arr,0,#arr.size()-1);
print(#arr);
print('--------------------------------');
#testArr.sort(lambda('o1','o2',@block{#o1-#o2}));
print(#testArr);

func('quickSort','arr','left','right',@block{
    if(#left>=#right){return;};
    def midVal=#arr[#left];
    def l=#left;
    def r=#right;
    for(i:condition(@block{#l<#r})){
        for(i1:condition(@block{#arr[#l]<#midVal})){#l++};
        for(i1:condition(@block{#arr[#r]>#midVal})){#r--};
        if(#l==#r){break};
        def temp=#arr[#l];
        #arr[#l]=#arr[#r];
        #arr[#r]=#temp;
        if(#arr[#l]==#midVal){#r--};
        if(#arr[#r]==#midVal){#l++};
    };
    #l--;
    #r++;
    quickSort(#arr,#left,#l);
    quickSort(#arr,#r,#right);
});