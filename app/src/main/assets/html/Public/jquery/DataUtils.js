let Base64 = {
    encode(str) {
        // first we use encodeURIComponent to get percent-encoded UTF-8,
        // then we convert the percent encodings into raw bytes which
        // can be fed into btoa.
        return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
            function toSolidBytes(match, p1) {
                return String.fromCharCode('0x' + p1);
            }));
    },
    decode(str) {
        // Going backwards: from bytestream, to percent-encoding, to original string.
        return decodeURIComponent(atob(str).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
    }
};

//let encoded = Base64.encode("哈ha"); // "5ZOIaGE="
//let decoded = Base64.decode(encoded); // "哈ha"
class DataUtils{
    jsData={};
    put(key,value){
        this.jsData[key]=value;
     }

     get(key){
         return this.jsData[key]
     }
     parse(str){
         const myURL = new URL(str);
       //  console.log(myURL)
         const searchParams = new URLSearchParams(myURL.search);
        // console.log(searchParams)
         const that = this;
         searchParams.forEach(function(k,v){
            //encodeURIComponent(Base64.encode(
          //   console.log(k,v)
             that.jsData[v]=Base64.decode(decodeURIComponent(k));
        })
    }
    toString(){
        let str1 = "data://string?";
        for(const i in this.jsData) {
            str1+="&"+i+"="+encodeURIComponent(Base64.encode(this.jsData[i]));
       }
        return str1;
    }
}
