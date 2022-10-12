pipeline {
    agent {
        label 'main-node'
    }
    
    tools {
        jdk 'openjdk-17'
    }
    stages {
        stage('setup') {
            
            steps {
              sh '''
              wget https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -O awscliv2.zip
              unzip -o awscliv2.zip
              ./aws/install
              aws configure set aws_access_key_id access_key_id
              aws configure set aws_secret_access_key access_secret_key
              aws configure set default.region us-west-2
              aws configure set default.output json
              aws ec2 run-instances \
              --image-id ami-XXXXXXXXXXXXX --count 1 \
              --instance-type c5.xlarge --key-name KeyName \
              --security-group-ids sg-XXXXXXXX \
              --subnet-id subnet-XXXXXXXXXXXX \
              --iam-instance-profile Name="IAMProfileName" \
              --associate-public-ip-address \
              --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=200, VolumeType=gp2, DeleteOnTermination=true}' \
              --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=Performance_Testing}]' > InstanceDetails.txt

              '''
            }
        }

        stage('setting_up'){
           steps{
              sh 'sleep 40s'
           }
        }

        stage('terminating_instance'){
           steps{
            sh '''
              aws ec2 terminate-instances --instance-ids "$(aws ec2 describe-instances --filters "Name=tag:Name,Values=Performance_Testing" "Name=instance-state-name,Values=running" --query "Reservations[].Instances[].InstanceId")"
              '''
           }
        }
    }
}
