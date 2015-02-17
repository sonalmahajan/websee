var cssdict = {};
var getElementByXpath = function(path) {
    return document.evaluate(path, document, null, 9, null).singleNodeValue;
};
element = getElementByXpath(arguments[0]);
if (element.style) {
  for (var k = 0; k < element.style.length; k++) {
    var property = element.style.item(k);
    if (property.indexOf("-webkit") == -1) {
      cssdict[property] = element.style.getPropertyValue(property);
    }
  }
}
return cssdict;
