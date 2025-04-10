//
//  PrintJobInfo.swift
//  flutter_downloader
//
//  Created by Lorenzo Pichilli on 10/05/22.
//

import Foundation

public class PrintJobInfo: NSObject {
    var state: PrintJobState
    var attributes: PrintAttributes
    var creationTime: Int64
    var numberOfPages: Int?
    var label: String?
    var printerId: String?
    
    public init(fromPrintJobController: PrintJobController) {
        state = fromPrintJobController.state
        creationTime = fromPrintJobController.creationTime
        attributes = PrintAttributes.init(fromPrintJobController: fromPrintJobController)
        super.init()
        if let printPageRenderer = fromPrintJobController.printPageRenderer {
            numberOfPages = printPageRenderer.numberOfPages
        }
        if let job = fromPrintJobController.job, let printInfo = job.printInfo {
            label = printInfo.jobName
            printerId = printInfo.printerID
        }
    }
    
    public func toMap () -> [String:Any?] {
        // Only include properties that are available across all iOS versions
        var map: [String: Any?] = [
            "state": state.rawValue,
            "attributes": attributes.toMap(),
            "creationTime": creationTime,
            "label": label
        ]
        
        if let pages = numberOfPages {
            map["numberOfPages"] = pages
        }
        if let pId = printerId {
            map["printer"] = ["id": pId]
        }
        return map
    }
}
