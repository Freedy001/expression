

defClass('com.freedy.expression.HeepSorter',@block{
    def arr;

    func('construct','arr',@block{
        @arr=arr;
    });


    func('sort',@block{
        for(i @ len arr / 2){
            adjust(i, len arr);
        };
        for(i @ range( 1, len arr -1 ) ){
            swap(0, i);
            adjust(0, i);
        };
        print(arr);
    });

    func('adjust','i','len',@block{
        def j=i*2+1;
        if (j >= len){  return; };
        if (j + 1 < len && arr[j] < arr[j + 1]) {
            j++;
        };
        if (arr[i] < arr[j]){ swap(i, j); };
        adjust(j, len);
    });


    //交换数组中下标为a和b的两个元素
    func('swap','a','b',@block{
        arr[a]=arr[a]^arr[b];
        arr[b]=arr[a]^arr[b];
        arr[a]=arr[a]^arr[b];
    });
});


def arr=[32,5,7,78,7,97,9,78,12,25,2,437,56,876,976];
def sorter=new('com.freedy.expression.HeepSorter',arr);
sorter.sort();


