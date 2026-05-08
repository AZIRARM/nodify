function checkPosts(){
  const translationParam = getRequestParam("translation");
  const url = "$value(BASE_URL)/contents/node/code/CONTENTS-nodifyheadlesscmsgmailcom-9340196424?payloadOnly=true"+(translationParam ? `&translation=${translationParam}` : "");
  $.get(url, function(data, status){
   console.log(data);
   if(data && data.length > 0){
      $('#posts').html("");
       data.forEach(post=>{
           if(post.title){
              $('#posts').append('<div class="container mt-5"><div class="custom-card"  style="margin-bottom: 50px;">'+ 
                  '<div class="custom-card-header">'+post.title+'</div>'+
              '<img src="$value(BASE_URL)/contents/code/'+post.picture+'/file" class="card-img-top">'+
              '<div class="custom-card-body">'+post.content+'</div></div></div>');
           }
       });
   }
  });
}

function getRequestParam(name) {
  const match = new RegExp('[?&]' + encodeURIComponent(name) + '=([^&]*)').exec(location.search);
  return match ? decodeURIComponent(match[1]) : null;
}


window.addEventListener('load', function () {
  function waitForjQuery(callback) {
    if (window.jQuery) {
      callback();
    } else {
      setTimeout(() => waitForjQuery(callback), 50);
    }
  }

  waitForjQuery(function () {
    console.log('✅ Page et jQuery chargés');
    
    $(document).ready(function() {
      setInterval(function() {
        checkPosts();
      }, 30000);
    });

    checkPosts();
  });
});