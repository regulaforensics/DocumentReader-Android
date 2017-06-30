package com.regula.documentreader.demo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.regula.documentreader.api.enums.LCID;
import com.regula.documentreader.api.enums.eRPRM_FieldVerificationResult;
import com.regula.documentreader.api.enums.eRPRM_ResultType;
import com.regula.documentreader.api.results.DocumentReaderTextField;
import com.regula.documentreader.api.results.DocumentReaderValue;
import com.regula.documentreader.api.translation.TranslationUtil;

import java.util.List;

public class TextDataAdapter extends ArrayAdapter<DocumentReaderTextField> {

    private boolean hasMrz, hasRfid, hasBarcode, hasVisual;
    private Context context;

    public TextDataAdapter(Context context, int resource, List<DocumentReaderTextField> objects, View listViewHeader) {
        super(context, resource, objects);
        this.context = context.getApplicationContext();

        for (int i = 0; i < objects.size(); i++) {
            DocumentReaderTextField obj = objects.get(i);

            if(!hasMrz){
                for(DocumentReaderValue value : obj.values){
                    if(value.sourceType== eRPRM_ResultType.RPRM_ResultType_MRZ_OCR_Extended)
                    {
                        hasMrz = true;
                        break;
                    }
                }
            }
            if(!hasBarcode){
                for(DocumentReaderValue value : obj.values){
                    if(value.sourceType==eRPRM_ResultType.RPRM_ResultType_BarCodes_TextData)
                    {
                        hasBarcode = true;
                        break;
                    }
                }
            }
            if(!hasVisual){
                for(DocumentReaderValue value : obj.values){
                    if(value.sourceType==eRPRM_ResultType.RPRM_ResultType_Visual_OCR_Extended)
                    {
                        hasVisual = true;
                        break;
                    }
                }
            }
        }

        if (!hasMrz) {
            listViewHeader.findViewById(R.id.headerMrzBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerMrzRfidTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerMrzTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerMrzVisualTv).setVisibility(View.GONE);
        }
        if (!hasRfid) {
            listViewHeader.findViewById(R.id.headerRfidBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerRfidTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerRfidVisualTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerMrzRfidTv).setVisibility(View.GONE);
        }
        if (!hasBarcode) {
            listViewHeader.findViewById(R.id.headerMrzBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerRfidBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerVisualBarcodeTv).setVisibility(View.GONE);
        }
        if (!hasVisual) {
            listViewHeader.findViewById(R.id.headerVisualBarcodeTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerVisualTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerRfidVisualTv).setVisibility(View.GONE);
            listViewHeader.findViewById(R.id.headerMrzVisualTv).setVisibility(View.GONE);
        }

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.text_fields_data_layout, null);
        }

        DocumentReaderTextField p = getItem(position);
        if (p != null) {
            TextView name = (TextView) v.findViewById(R.id.nameTv);
            TextView mrz = (TextView) v.findViewById(R.id.mrzTv);
            TextView barcode = (TextView) v.findViewById(R.id.barcodeTv);
            TextView rfid = (TextView) v.findViewById(R.id.rfidTv);
            TextView visual = (TextView) v.findViewById(R.id.visualTv);

            ImageView overall = (ImageView) v.findViewById(R.id.overallResultIv);
            ImageView mrzrfid = (ImageView) v.findViewById(R.id.mrzRfidIv);
            ImageView mrzBarcode = (ImageView) v.findViewById(R.id.mrzBarcodeIv);
            ImageView mrzvisual = (ImageView) v.findViewById(R.id.mrzVisualIv);
            ImageView visualBarcode = (ImageView) v.findViewById(R.id.barcodeVisualIv);
            ImageView visualrfid = (ImageView) v.findViewById(R.id.rfidVisualIv);
            ImageView rfidBarcode = (ImageView) v.findViewById(R.id.rfidBarcodeIv);

            String fieldType = TranslationUtil.getTextFieldTranslation(context, p.fieldType).replace("^", "\n");
            if (p.lcid > 0)
                fieldType += "(" + LCID.LCIDS.get(p.lcid) + ")";
            name.setText(fieldType);

            mrz.setText("");
            barcode.setText("");
            visual.setText("");
            int mrzVisual = eRPRM_FieldVerificationResult.RCF_Disabled, barcodeMrz= eRPRM_FieldVerificationResult.RCF_Disabled, barcodeVisual= eRPRM_FieldVerificationResult.RCF_Disabled;
            for(DocumentReaderValue val:p.values){
                if(val.sourceType==eRPRM_ResultType.RPRM_ResultType_MRZ_OCR_Extended){
                    mrz.setText(val.value.replace("^", "\n"));
                    mrz.setTextColor(val.validity == 0 ? Color.BLACK : val.validity == 1 ? Color.rgb(3, 140, 7) : Color.RED);
                    mrzVisual = val.comparison.get(eRPRM_ResultType.RPRM_ResultType_Visual_OCR_Extended);
                    barcodeMrz = val.comparison.get(eRPRM_ResultType.RPRM_ResultType_BarCodes_TextData);
                } else if(val.sourceType==eRPRM_ResultType.RPRM_ResultType_BarCodes_TextData){
                    barcode.setText(val.value.replace("^", "\n"));
                    barcode.setTextColor(val.validity == 0 ? Color.BLACK : val.validity == 1 ? Color.rgb(3, 140, 7) : Color.RED);
                    barcodeMrz = val.comparison.get(eRPRM_ResultType.RPRM_ResultType_BarCodes_TextData);
                    barcodeVisual= val.comparison.get(eRPRM_ResultType.RPRM_ResultType_Visual_OCR_Extended);
                } else if(val.sourceType==eRPRM_ResultType.RPRM_ResultType_Visual_OCR_Extended){
                    visual.setText(val.value.replace("^", "\n"));
                    visual.setTextColor(val.validity == 0 ? Color.BLACK : val.validity == 1 ? Color.rgb(3, 140, 7) : Color.RED);
                    barcodeVisual= val.comparison.get(eRPRM_ResultType.RPRM_ResultType_BarCodes_TextData);
                    mrzVisual = val.comparison.get(eRPRM_ResultType.RPRM_ResultType_MRZ_OCR_Extended);
                }
            }

//            mrzrfid.setImageResource(p.matrix.get(4) == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
//                    p.matrix.get(4) == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
            mrzBarcode.setImageResource(barcodeMrz == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
                    barcodeMrz == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
            mrzvisual.setImageResource(mrzVisual == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
                    mrzVisual == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
            visualBarcode.setImageResource(barcodeVisual == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
                    barcodeVisual == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
//            visualrfid.setImageResource(p.matrix.get(7) == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
//                    p.matrix.get(7) == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
//            rfidBarcode.setImageResource(p.matrix.get(9) == eRPRM_FieldVerificationResult.RCF_Compare_True ? R.drawable.ok :
//                    p.matrix.get(9) == eRPRM_FieldVerificationResult.RCF_Compare_False ? R.drawable.fail : R.drawable.undefined);
//
            overall.setImageResource(p.status == eRPRM_FieldVerificationResult.RCF_Disabled ? R.drawable.undefined
                    : p.status == eRPRM_FieldVerificationResult.RCF_Not_Verified ? R.drawable.fail : R.drawable.ok);

            if(!hasMrz){
                mrz.setVisibility(View.GONE);
                mrzBarcode.setVisibility(View.GONE);
                mrzrfid.setVisibility(View.GONE);
                mrzvisual.setVisibility(View.GONE);
            }
            if(!hasBarcode){
                barcode.setVisibility(View.GONE);
                visualBarcode.setVisibility(View.GONE);
                mrzBarcode.setVisibility(View.GONE);
                rfidBarcode.setVisibility(View.GONE);
            }
            if(!hasRfid){
                rfid.setVisibility(View.GONE);
                rfidBarcode.setVisibility(View.GONE);
                visualrfid.setVisibility(View.GONE);
                mrzrfid.setVisibility(View.GONE);
            }
            if(!hasVisual){
                visual.setVisibility(View.GONE);
                visualrfid.setVisibility(View.GONE);
                visualBarcode.setVisibility(View.GONE);
                mrzvisual.setVisibility(View.GONE);
            }

            v.setBackgroundColor(position % 2 > 0 ? Color.rgb(228, 228, 237) : Color.rgb(237, 237, 228));
        }
        return v;
    }
}
