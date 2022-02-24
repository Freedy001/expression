//二分查找非递归实现

def arr1=[23,54,12,65,8,2,6,7,8,9,234,5432,234,5,2,1,5678,8,23];
def arr2=[1,2,6,1003];

print(isSorted(arr2));
//binarySort(arr1,2);
//binarySort(arr2,98);

func('binarySort','arr','target',@block{
    if(!isSorted(arr)){
        return "arr is not isSorted";
    };
    def left=0;
    def right=arr.size()-1;
    def mid;
    for(i:condition(left <= right)){
        mid=(left+right)/2;
        if(arr[mid]==target){
            return mid;
        }else if (arr[mid]<target){
            left=mid+1;
        }else if (arr[mid]>target){
            right=mid+1;
        }
    };
    return 'not found';
});

func('isSorted','arr',@block{
    def last=T(java.lang.Integer).MIN_VALUE;
    for(i:arr){
        if(i>last){
            last=i;
        }else{
            return false;
        }
    };
    return true;
});