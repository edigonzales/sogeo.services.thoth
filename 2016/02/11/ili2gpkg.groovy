html.html {
    head {
        meta(charset:'utf-8')
        meta(name:'viewport', content:'width=device-width, initial-scale=1')
        meta('http-equiv':'x-ua-compatible', content:'ie=edge')   
         
        title 'ili2gpkg online converter'
        style '''
            label { display: block; padding: 0.2em; }
        '''
    }
    body {
        h1 'INTERLIS (ITF/XTF) to GeoPackage Converter'
        form action: 'do_ili2gpkg.groovy', method: 'post', enctype: 'multipart/form-data', {
            label 'Reference frame: ', {
                select name: 'reference_frame', {
                    option 'LV03'
                    option 'LV95'
                }
            }
            label "--strokeArcs", {
                input type: 'checkbox', checked: 'checked', name: 'strokeArcs', id: 'strokeArcs'
            }
            label "--skipPolygonBuilding", {
                input type: 'checkbox', name: 'skipPolygonBuilding', id: 'skipPolygonBuilding'
            }
            label "--nameByTopic", {
                input type: 'checkbox', checked: 'checked', name: 'nameByTopic', id: 'nameByTopic'
            }
            label 'File: ', {
                input type: 'file', name: 'file'
            }
            input type: 'submit', name: 'submit', value: 'Send to server'
        }
    }
}
