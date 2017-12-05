package com.regula.documentreader.demo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.regula.documentreader.api.enums.eGraphicFieldType;
import com.regula.documentreader.api.enums.eRPRM_FieldVerificationResult;
import com.regula.documentreader.api.enums.eVisualFieldType;

public class OverallResultFragment extends Fragment {

    private TextView dobTv, nationalityTv, docNumberTv, persNumberTv, doeTv, issuingStateNameTv, documentTypeTv;
    private ImageView frontPageIv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =   inflater.inflate(R.layout.overall_result_fragment,container, false);

        documentTypeTv = (TextView) view.findViewById(R.id.documentTypeTv);
        dobTv = (TextView) view.findViewById(R.id.dateOfBirthTv);
        nationalityTv = (TextView) view.findViewById(R.id.nationalityTv);
        docNumberTv = (TextView) view.findViewById(R.id.documentNumberTv);
        persNumberTv = (TextView) view.findViewById(R.id.personalNumberTv);
        doeTv = (TextView) view.findViewById(R.id.dateOfExpiryTv);
        issuingStateNameTv = (TextView) view.findViewById(R.id.issuingStateTv);

        frontPageIv = (ImageView) view.findViewById(R.id.frontPageIv);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(ResultsActivityTabbed.documentReaderResults!=null) {

            if( ResultsActivityTabbed.documentReaderResults.documentType!=null){
                documentTypeTv.setText(ResultsActivityTabbed.documentReaderResults.documentType.name);
            } else {
                documentTypeTv.setText(R.string.strUnknownDocType);
            }

            String dob = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Date_of_Birth);
            int dobStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Date_of_Birth);
            if(dob!=null) {
                dobTv.setText(dob);
                if(dobStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    dobTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(dobStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    dobTv.setTextColor(Color.RED);
                }
            }

            String nationality = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Nationality);
            int natStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Nationality);
            if(nationality!=null){
                nationalityTv.setText(nationality);
                if(natStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    nationalityTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(natStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    nationalityTv.setTextColor(Color.RED);
                }
            }

            String docNumber = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Document_Number);
            int docuNrStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Document_Number);
            if(docNumber!=null){
                docNumberTv.setText(docNumber);
                if(docuNrStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    docNumberTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(docuNrStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    docNumberTv.setTextColor(Color.RED);
                }
            }

            String personalNumber = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Personal_Number);
            int persNrStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Personal_Number);
            if(personalNumber!=null){
                persNumberTv.setText(personalNumber);
                if(persNrStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    persNumberTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(persNrStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    persNumberTv.setTextColor(Color.RED);
                }
            }

            String dateOfExpiry = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Date_of_Expiry);
            int doeStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Date_of_Expiry);
            if(dateOfExpiry!=null){
                doeTv.setText(dateOfExpiry);
                if(doeStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    doeTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(doeStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    doeTv.setTextColor(Color.RED);
                }
            }

            String issuingState = ResultsActivityTabbed.documentReaderResults.getTextFieldValueByType(eVisualFieldType.ft_Issuing_State_Name);
            int issuingStateStatus = ResultsActivityTabbed.documentReaderResults.getTextFieldStatusByType(eVisualFieldType.ft_Issuing_State_Name);
            if(issuingState!=null){
                issuingStateNameTv.setText(issuingState);
                if(issuingStateStatus == eRPRM_FieldVerificationResult.RCF_Verified){
                    issuingStateNameTv.setTextColor(Color.rgb(3, 140, 7));
                } else if(issuingStateStatus== eRPRM_FieldVerificationResult.RCF_Not_Verified){
                    issuingStateNameTv.setTextColor(Color.RED);
                }
            }

            Bitmap frontPage = ResultsActivityTabbed.documentReaderResults.getGraphicFieldImageByType(eGraphicFieldType.gt_Document_Front);
            if(frontPage!=null){
                frontPageIv.setImageBitmap(frontPage);
            }
        }
    }
}
