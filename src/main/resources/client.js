

let websocket = new WebSocket('ws://localhost:8080/wire');


websocket.onmessage = (event) => {
    console.log(event.data)
    if(event.data[0] == 'a') parseAttributeCommand(event.data)
    else parseTagCommand(event.data)
}
function debounce(callback, delay) {
    let timer
    return function(...args) {
        clearTimeout(timer)
        timer = setTimeout(() => {
            callback(...args);
        }, delay)
    }
}
const e = debounce((evId,value)=> {
    if(value) websocket.send(evId+"$"+value)
    else websocket.send(evId)
},10)

function removeAttribute(id, attr) {
    const element = document.getElementById(id)
    element.removeAttribute(attr);
    console.log("Received command: Remove Attribute '",attr,"' from ", element);
}

function setAttribute(id, attribute, value) {
    let element = document.getElementById(id);
    element.setAttribute(attribute, value);
    console.log("Received command: Set attribute ("+attribute+"="+ value+") on ",element);
}
function removeChild(id) {
    let text = textIds[id]
    let toRemove;
    if(text) {
        toRemove = text
        textIds[id] = undefined
    } else toRemove = document.getElementById(id);
    toRemove.remove();
    console.log("Received command: Remove node(",id,") ",toRemove)
}

function parseAttributeCommand(data) {
    let id = "";
    let i = 1;
    let attrStartIndex;
    for (; i < data.length; i++) {
        if (data.charAt(i) == '-') {
            add = false;
            removeAttribute(data.substring(1, i), data.substring(i + 1));
            return;
        }
        if (data.charAt(i) == '+') {
            add = true;
            id = data.substring(1, i);
            attrStartIndex = i+1
            break;
        }
    }
    for (; i < data.length; i++) {
        if (data.charAt(i) == '=') {
            let attribute = data.substring(attrStartIndex,i);
            let value = data.substring(i+1);
            setAttribute(id, attribute, value);
            return;
        }
    }
}


function insertTag(par_id, tag, id, slot, offset) {
    let parent = document.getElementById(par_id);
    let child = document.createElement(tag);
    child.id = id;
    child.setAttribute("ktx-slot", slot)
    let previousChild = findNextChild(parent, slot, offset);
    if(previousChild) parent.insertBefore(child, previousChild)
    else parent.appendChild(child)
    console.log("Command received: Insert Child tag ", child, " into ", parent,"(before ",previousChild,")")
}

const textIds = {}
function insertText(par_id, text,id, slot, offset) {
    let parent = document.getElementById(par_id);
    let child = document.createTextNode(text);
    child.slot = slot;
    textIds[id] = child;
    let previousChild = findNextChild(parent, slot, offset);
    if(previousChild) parent.insertBefore(child, previousChild)
    else parent.appendChild(child)
    console.log("Command received: Insert Text(id=",id,", slot=",slot,", offset=",offset,") ", child, " into ", parent,"(before ",previousChild,")")
}

/**
 * @param parent {HTMLElement}
 * @param slot
 * @param offset
 * @return {Node | null}
 */
function findNextChild(parent, slot, offset) {
    /**@type {Number | null}*/
    let prev = null;
    for (let i = parent.childNodes.length - 1; i >= 0; i--) {
        let child = parent.childNodes[i];
        let childSlot;
        if(child instanceof Text) {
            childSlot = child.slot;
        } else if(child instanceof Element) {
            childSlot = child.getAttribute("ktx-slot");
        }
        if(childSlot != null && childSlot <= slot) break;
        prev = i;
    }
    if(offset !== null && !isNaN(+offset) && offset !== "") {
        let elementsInSlot = 0
        for (let i = (prev != null)?prev-1: parent.childNodes.length-1; i >= 0; i--) {
            let child = parent.childNodes[i];
            let childSlot;
            if(child instanceof Text) {
                childSlot = child.slot;
            } else if(child instanceof Element) {
                childSlot = child.getAttribute("ktx-slot");
            }
            if(childSlot === slot) elementsInSlot++;
            if(childSlot < slot) break;
        }
        const reverseOffset = elementsInSlot - offset
        if(reverseOffset < 0) throw Error("offset is higher than elements in slot")
        if(prev != null) prev = prev - reverseOffset
        else if(reverseOffset>0) prev = parent.childNodes.length-reverseOffset
    }
    return prev != null ? parent.childNodes[prev] : null
}

function parseTagCommand(data) {
    let slot = "";
    let offset = "";
    let i = 0;
    let hasOffset = false;
    for (; i < data.length; i++) {
        if (data.charAt(i) == '-') {
            removeChild(data.substring(i+1))
            return;
        }
        if (data.charAt(i) == '+') {
            i++;
            break;
        }
        if (data.charAt(i) == ',') {
            hasOffset = true;
            continue;
        }
        if(!hasOffset) slot += data.charAt(i);
        else offset += data.charAt(i);
    }
    let par_id = "";
    let mode = "";
    for (; i < data.length; i++) {
        if (data.charAt(i) === "t") {
            mode = "t"
            break;
        }
        if (data.charAt(i) === "s") {
            mode = "s"
            break;
        }
        par_id += data.charAt(i);
    }
    let symb = data.substring(++i);
    if(mode === "t") {
        let parts = symb.split("_");
        let id = parts[0]
        let tag = parts[1];
        insertTag(par_id, tag, id, slot, offset);
    } else if(mode === "s") {
        let parts = symb.split("_");
        let id = parts[0]
        let text = parts[1];
        insertText(par_id, text, id, slot, offset);
    }
}
